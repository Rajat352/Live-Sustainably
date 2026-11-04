package com.livesustainably.ui.screens.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.livesustainably.domain.model.UserProfile
import com.livesustainably.R
import com.livesustainably.ui.common.dataErrorMessage
import com.livesustainably.ui.common.formatPostDate
import com.livesustainably.ui.common.formatSaving
import com.livesustainably.ui.components.AppCard
import com.livesustainably.ui.components.EmptyContent
import com.livesustainably.ui.components.ErrorContent
import com.livesustainably.ui.components.LoadingContent

@Composable
fun FeedScreen(
    state: FeedUiState,
    onAction: (FeedAction) -> Unit,
    onComments: (PostKey) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.feed_title), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.feed_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.feed_description), style = MaterialTheme.typography.bodyMedium)
            }
        }
        when {
            state.isLoading -> item { LoadingContent(stringResource(R.string.feed_loading)) }
            state.error != null -> item { ErrorContent(dataErrorMessage(state.error), { onAction(FeedAction.Retry) }) }
            state.posts.isEmpty() -> item {
                EmptyContent(stringResource(R.string.feed_empty_title), stringResource(R.string.feed_empty_message))
            }
            else -> items(state.posts, key = { "${it.key.authorId}/${it.key.postId}" }) { item ->
                FeedPostCard(item, state, onAction, onComments)
            }
        }
    }
}

@Composable
private fun FeedPostCard(
    item: FeedItemUiState,
    state: FeedUiState,
    onAction: (FeedAction) -> Unit,
    onComments: (PostKey) -> Unit,
) {
    val liked = state.userId in item.likes
    val pending = item.key in state.pendingLikes

    if (item.isUnavailable) {
        ErrorContent(
            dataErrorMessage(requireNotNull(item.accessError)),
            onRetry = {
                if (item.authorError != null) onAction(FeedAction.RetryAuthor(item.post.authorId))
                if (item.likesError != null) onAction(FeedAction.RetryLikes(item.key))
            },
        )
        return
    }

    AppCard {
        if (item.isLoadingAuthor) Text(stringResource(R.string.feed_loading_author))
        else AuthorIdentity(item.author, item.post.authorId == state.userId)
        item.authorError?.let {
            Text(stringResource(R.string.feed_profile_error, dataErrorMessage(it)), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { onAction(FeedAction.RetryAuthor(item.post.authorId)) }) { Text(stringResource(R.string.feed_retry_profile)) }
        }
        Text(formatPostDate(item.post.createdAtMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (item.post.isDemo) {
            Text(stringResource(R.string.feed_demo_estimate), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        }
        Text(item.post.description, style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.feed_saving, formatSaving(item.post.savedKgCo2e)), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        val likedDescription = stringResource(if (liked) R.string.feed_liked else R.string.feed_not_liked)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                onClick = { onAction(FeedAction.ToggleLike(item.key)) },
                enabled = !pending && !item.isLoadingLikes && item.likesError == null,
                modifier = Modifier.semantics { stateDescription = likedDescription },
            ) {
                Text(when {
                    pending -> stringResource(R.string.feed_saving_like)
                    item.isLoadingLikes -> stringResource(R.string.feed_loading_likes)
                    item.likesError != null -> stringResource(R.string.feed_likes_unavailable)
                    else -> stringResource(if (liked) R.string.feed_unlike_count else R.string.feed_like_count, item.likes.size)
                })
            }
            TextButton(onClick = { onComments(item.key) }) { Text(stringResource(R.string.comments_title)) }
        }
        item.likesError?.let {
            Text(stringResource(R.string.feed_likes_error, dataErrorMessage(it)), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { onAction(FeedAction.RetryLikes(item.key)) }) { Text(stringResource(R.string.feed_retry_likes)) }
        }
        state.likeErrors[item.key]?.let { message ->
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(stringResource(R.string.feed_like_not_confirmed, dataErrorMessage(message)), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { onAction(FeedAction.RetryLike(item.key)) }, enabled = !pending) { Text(stringResource(R.string.feed_retry_like)) }
            }
        }
    }
}

@Composable
private fun AuthorIdentity(profile: UserProfile?, isYou: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val avatar = when (profile?.avatarId) {
            "tree" -> "🌳"
            "sun" -> "☀"
            "earth" -> "🌍"
            else -> "🌿"
        }
        Text(avatar, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 2.dp))
        Text(
            if (isYou) stringResource(R.string.author_you, profile?.displayName ?: stringResource(R.string.your_account))
            else profile?.displayName ?: stringResource(R.string.community_member),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
