package com.livesustainably.ui.screens.comments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livesustainably.ui.components.AppButton
import com.livesustainably.ui.components.AppCard
import com.livesustainably.ui.components.EmptyContent
import com.livesustainably.ui.components.ErrorContent
import com.livesustainably.ui.components.LoadingContent
import com.livesustainably.R
import com.livesustainably.ui.common.formatPostDate
import com.livesustainably.ui.common.dataErrorMessage

@Composable
fun CommentsScreen(
    state: CommentsUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
) {
    val listState = rememberLazyListState()
    val latestCommentId = state.comments.lastOrNull()?.id
    LaunchedEffect(latestCommentId) {
        if (latestCommentId != null) listState.animateScrollToItem(state.comments.lastIndex)
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Text(stringResource(R.string.comments_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 24.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.isLoading -> item { LoadingContent(stringResource(R.string.comments_loading)) }
                state.loadError != null -> item { ErrorContent(dataErrorMessage(state.loadError), onRetry) }
                state.comments.isEmpty() -> item { EmptyContent(stringResource(R.string.comments_empty_title), stringResource(R.string.comments_empty_message)) }
                else -> items(state.comments, key = { it.id }) { comment ->
                    AppCard {
                        Text(stringResource(if (comment.authorId == state.userId) R.string.comment_you else R.string.community_member), style = MaterialTheme.typography.titleMedium)
                        Text(comment.text)
                        Text(
                            if (comment.id == state.unconfirmedId) stringResource(R.string.comment_awaiting_confirmation) else formatPostDate(comment.createdAtMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.sendError?.let {
                Text(stringResource(R.string.comment_not_confirmed, dataErrorMessage(it)), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            OutlinedTextField(
                value = state.draft,
                onValueChange = onDraftChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.comment_label)) },
                enabled = !state.isSending && state.userId != null,
                isError = state.draftError || state.draft.trim().length > 500,
                supportingText = { Text(if (state.draftError) stringResource(R.string.comment_validation) else stringResource(R.string.comment_counter, state.draft.trim().length)) },
                minLines = 1,
                maxLines = 3,
            )
            AppButton(
                text = when {
                    state.isSending -> stringResource(R.string.comment_sending)
                    state.sendError != null -> stringResource(R.string.comment_retry)
                    else -> stringResource(R.string.comment_send)
                },
                onClick = onSend,
                enabled = !state.isSending && !state.isLoading && state.loadError == null && state.userId != null,
            )
        }
    }
}
