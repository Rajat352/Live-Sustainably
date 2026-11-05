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
import com.livesustainably.ui.common.displayCommunityName
import com.livesustainably.ui.common.displayActivityDescription
import com.livesustainably.ui.common.formatPostDate
import com.livesustainably.ui.common.formatSaving
import com.livesustainably.ui.components.AppCard
import com.livesustainably.ui.components.HeroCard
import com.livesustainably.ui.components.ScreenHeading
import com.livesustainably.ui.components.AvatarBadge
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import com.livesustainably.ui.components.EmptyContent
import com.livesustainably.ui.components.ErrorContent
import com.livesustainably.ui.components.LoadingContent

@Composable
fun FeedScreen(
    state: FeedUiState,
    onAction: (FeedAction) -> Unit,
    onChat: () -> Unit,
    onComments: (PostKey) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeading(stringResource(R.string.feed_title), stringResource(R.string.feed_subtitle), "GOOD THINGS GROW TOGETHER")
        }

        item {
            HeroCard("Small steps.\nHappy planet.", "A greener commute, a thoughtful meal. Every little choice counts.") {
                TextButton(
                    onClick = onChat,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    modifier = Modifier.background(Color.White.copy(alpha = 0.18f), CircleShape),
                ) { Text("Meet Sprout, your green guide  →") }
            }
        }

        item { Text("Fresh from your circle", style = MaterialTheme.typography.titleLarge) }

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
        Text(displayActivityDescription(item.post.description, item.post.isDemo), style = MaterialTheme.typography.bodyLarge)
        Text("🌱  " + stringResource(R.string.feed_saving, formatSaving(item.post.savedKgCo2e)),
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape).padding(horizontal = 14.dp, vertical = 9.dp))
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
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AvatarBadge(profile?.avatarId)
        Text(
            if (isYou) stringResource(R.string.author_you, profile?.displayName?.let(::displayCommunityName) ?: stringResource(R.string.your_account))
            else profile?.displayName?.let(::displayCommunityName) ?: stringResource(R.string.community_member),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
