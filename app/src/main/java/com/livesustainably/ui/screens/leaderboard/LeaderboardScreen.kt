package com.livesustainably.ui.screens.leaderboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livesustainably.R
import com.livesustainably.ui.common.dataErrorMessage
import com.livesustainably.ui.common.isAccessDenied
import com.livesustainably.ui.components.*
import com.livesustainably.ui.screens.reports.formatReportWindow

@Composable
fun LeaderboardScreen(
    state: LeaderboardUiState,
    onRetry: () -> Unit
) {

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(stringResource(R.string.leaderboard_title), style = MaterialTheme.typography.headlineLarge)
            Text(formatReportWindow(state.window), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.reports_timezone, state.window.zone.id), style = MaterialTheme.typography.bodySmall)
        }

        item { AppCard { Text(stringResource(R.string.leaderboard_explanation)) } }

        when {
            state.isLoading -> item { LoadingContent(stringResource(R.string.leaderboard_loading)) }
            state.error != null -> item { ErrorContent(dataErrorMessage(state.error), onRetry) }
            else -> {
                if (state.rows.size == 1) item {
                    EmptyContent(stringResource(R.string.leaderboard_empty_title), stringResource(R.string.leaderboard_empty_message))
                }
                items(state.rows, key = { it.score.userId }) { row ->
                    AppCard(
                        Modifier.semantics(mergeDescendants = true) {}
                    ) {
                        if (row.profileError?.isAccessDenied == true) {
                            Text(dataErrorMessage(row.profileError))
                            AppButton(stringResource(R.string.leaderboard_retry), onRetry)
                        } else {
                            Text(stringResource(R.string.leaderboard_rank, row.score.rank), style = MaterialTheme.typography.labelLarge)
                            val name = row.profile?.displayName ?: stringResource(if (row.score.isYou) R.string.your_account else R.string.community_member)
                            Text(
                                if (row.score.isYou) stringResource(R.string.author_you, name) else name,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (row.score.isYou) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(stringResource(R.string.reports_points, row.score.points), style = MaterialTheme.typography.headlineSmall)
                            if (row.score.isDemo) Text(stringResource(R.string.leaderboard_demo), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
