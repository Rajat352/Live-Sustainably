package com.livesustainably.data.repository

import android.content.Context
import com.livesustainably.data.local.ChatCatalog
import com.livesustainably.data.local.ChatReplyKind
import com.livesustainably.domain.model.*
import com.livesustainably.domain.repository.ActivityRepository
import com.livesustainably.domain.repository.AuthRepository
import com.livesustainably.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocalChatRepository internal constructor(
    private val loadCatalog: () -> String,
    private val auth: AuthRepository,
    private val activities: ActivityRepository,
    private val clock: Clock,
) : ChatRepository {
    @Inject constructor(
        @ApplicationContext context: Context,
        auth: AuthRepository,
        activities: ActivityRepository,
        clock: Clock,
    ) : this({ context.assets.open("chat_catalog.json").bufferedReader().use { it.readText() } }, auth, activities, clock)

    private val catalog by lazy { ChatCatalog.parse(loadCatalog()) }

    override suspend fun reply(message: String): String {
        require(message.trim().length in 1..500)
        val (intent, fallback) = withContext(Dispatchers.IO) {
            try {
                catalog.match(message) to catalog.fallback
            } catch (error: Exception) {
                throw DataException(DataFailure.InvalidData, error)
            }
        }
        return when (intent?.kind) {
            null -> fallback
            ChatReplyKind.Text -> intent.reply
            ChatReplyKind.Week -> personalReport(ReportPeriod.Week)
            ChatReplyKind.Month -> personalReport(ReportPeriod.Month)
        }
    }

    private suspend fun personalReport(period: ReportPeriod): String = try {
        withTimeoutOrNull(8_000) {
            val session = auth.session.first { it != UserSession.Loading } as? UserSession.SignedIn
                ?: return@withTimeoutOrNull "Sign in again to read your personal report."
            val window = ReportSelection(period, LocalDate.now(clock)).window(clock.zone)
            val history = activities.observeActivities(session.userId, window.startMillis, window.endMillis).first()
            if (auth.session.first { it != UserSession.Loading } != session) throw DataException(DataFailure.SignedOut)
            formatChatReport(buildReport(history, window))
        } ?: "Your personal report is unavailable right now. Check your connection and try again. General sustainability advice still works offline."
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        when ((error as? DataException)?.reason) {
            DataFailure.SignedOut -> "Sign in again to read your personal report."
            DataFailure.PermissionDenied -> "Your personal report is not accessible. Open Reports to check access or try again."
            else -> "I couldn't load your personal report. Check your connection or open Reports, then try again. I won't estimate missing totals."
        }
    }
}

internal fun formatChatReport(report: SustainabilityReport): String {
    val total = report.total
    val saving = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }.format(total.savedKgCo2e)
    val dates = "${report.window.startDate} to ${report.window.endDateExclusive.minusDays(1)} (${report.window.zone.id})"
    val count = if (total.activityCount == 1) "1 activity" else "${total.activityCount} activities"
    val summary = if (total.activityCount == 0) "No activities are stored for $dates."
        else "For $dates, your activities total $saving kg CO₂e saved, ${total.points} points, and $count across all categories."
    return summary + " Open Reports to explore the breakdown."
}
