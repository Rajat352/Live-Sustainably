package com.livesustainably.domain.model

import androidx.compose.runtime.Stable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.serialization.Serializable

@Serializable
enum class ReportPeriod {
    Week,
    Month
}

data class ReportSelection(
    val period: ReportPeriod,
    val anchorDate: LocalDate
) {
    fun window(zone: ZoneId): ReportWindow {
        val start = when (period) {
            ReportPeriod.Week -> anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ReportPeriod.Month -> anchorDate.withDayOfMonth(1)
        }
        val end = when (period) {
            ReportPeriod.Week -> start.plusWeeks(1)
            ReportPeriod.Month -> start.plusMonths(1)
        }
        return ReportWindow(period, start, end, zone)
    }

    fun shifted(amount: Int): ReportSelection = copy(anchorDate = when (period) {
        ReportPeriod.Week -> anchorDate.plusWeeks(amount.toLong())
        ReportPeriod.Month -> anchorDate.withDayOfMonth(1).plusMonths(amount.toLong())
    })
}

data class ReportWindow(
    val period: ReportPeriod,
    val startDate: LocalDate,
    val endDateExclusive: LocalDate,
    val zone: ZoneId,
) {
    val startMillis: Long get() = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis: Long get() = endDateExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
    val periodKey: String get() = when (period) {
        ReportPeriod.Week -> "week_$startDate"
        ReportPeriod.Month -> "month_${startDate.toString().take(7)}"
    }
}

data class ReportTotal(
    val savedKgCo2e: Double,
    val points: Long,
    val activityCount: Int
)

data class DailyReport(
    val date: LocalDate,
    val total: ReportTotal
)

@Stable
data class SustainabilityReport(
    val window: ReportWindow,
    val total: ReportTotal,
    val days: List<DailyReport>,
    val categories: Map<ActivityCategory, ReportTotal>,
    val containsDemo: Boolean,
)

fun buildReport(
    activities: List<SustainabilityActivity>,
    window: ReportWindow,
    category: ActivityCategory? = null,
): SustainabilityReport {
    val selected = activities.filter {
        it.createdAtMillis >= window.startMillis && it.createdAtMillis < window.endMillis &&
            (category == null || it.category == category)
    }.distinctBy { it.id }
    val byDay = selected.groupBy { Instant.ofEpochMilli(it.createdAtMillis).atZone(window.zone).toLocalDate() }
    val days = generateSequence(window.startDate) { it.plusDays(1) }
        .takeWhile { it < window.endDateExclusive }
        .map { DailyReport(it, byDay[it].orEmpty().total()) }.toList()
    return SustainabilityReport(
        window, selected.total(), days,
        ActivityCategory.entries.associateWith { c -> selected.filter { it.category == c }.total() },
        selected.any { it.isDemo },
    )
}

private fun List<SustainabilityActivity>.total() = ReportTotal(
    sumOf { it.savedKgCo2e }, sumOf { it.points.toLong() }, size,
)

data class RankedScore(
    val rank: Int,
    val userId: String,
    val points: Long,
    val isDemo: Boolean,
    val isYou: Boolean
)

fun rankScores(userId: String, report: SustainabilityReport, peers: List<PeerScore>): List<RankedScore> {
    val scores = peers.filter { it.userId != userId }.distinctBy { it.userId }
        .map { RankedScore(0, it.userId, it.points.toLong(), it.isDemo, false) } +
        RankedScore(0, userId, report.total.points, report.containsDemo, true)
    var rank = 0
    var previousPoints: Long? = null
    return scores.sortedWith(compareByDescending<RankedScore> { it.points }.thenBy { it.userId })
        .mapIndexed { index, score ->
            if (score.points != previousPoints) rank = index + 1
            previousPoints = score.points
            score.copy(rank = rank)
        }
}
