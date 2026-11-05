package com.livesustainably.ui.screens.leaderboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livesustainably.R
import com.livesustainably.ui.common.dataErrorMessage
import com.livesustainably.ui.common.displayCommunityName
import com.livesustainably.ui.common.isAccessDenied
import com.livesustainably.ui.components.*
import com.livesustainably.ui.screens.reports.formatReportWindow

@Composable
fun LeaderboardScreen(
    state: LeaderboardUiState,
    onRetry: () -> Unit
) {

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard("Good company.\nGreat impact.", formatReportWindow(state.window)) {
                Text("🏆  " + stringResource(R.string.leaderboard_title),
                    style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color.White)
            }
        }

        item {
            ScreenHeading("Your growing circle", stringResource(R.string.leaderboard_explanation))
        }

        when {
            state.isLoading -> item { LoadingContent(stringResource(R.string.leaderboard_loading)) }
            state.error != null -> item { ErrorContent(dataErrorMessage(state.error), onRetry) }
            else -> {
                if (state.rows.size == 1) item {
                    EmptyContent(stringResource(R.string.leaderboard_empty_title), stringResource(R.string.leaderboard_empty_message))
                }
                items(state.rows, key = { it.score.userId }) { row ->
                    val name = row.profile?.displayName?.let(::displayCommunityName) ?: stringResource(if (row.score.isYou) R.string.your_account else R.string.community_member)
                    Surface(
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                        shape = RoundedCornerShape(26.dp),
                        color = if (row.score.isYou) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(3.dp, if (row.score.isYou) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        if (row.profileError?.isAccessDenied == true) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(dataErrorMessage(row.profileError))
                                AppButton(stringResource(R.string.leaderboard_retry), onRetry)
                            }
                        } else {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(when (row.score.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> row.score.rank.toString() },
                                        style = MaterialTheme.typography.titleLarge)
                                    AvatarBadge(row.profile?.avatarId, size = 44.dp)
                                    Column(Modifier.weight(1f)) {
                                        Text(if (row.score.isYou) stringResource(R.string.author_you, name) else name,
                                            style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(R.string.leaderboard_rank, row.score.rank), style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("⭐  " + stringResource(R.string.reports_points, row.score.points),
                                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                row.profileError?.let {
                                    Text(stringResource(R.string.feed_profile_error, dataErrorMessage(it)))
                                    AppButton(stringResource(R.string.leaderboard_retry), onRetry)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
