package com.livesustainably.ui.screens.feed

import androidx.compose.runtime.Stable
import com.livesustainably.domain.model.FeedPost
import com.livesustainably.domain.model.UserProfile
import com.livesustainably.domain.model.DataFailure
import com.livesustainably.ui.common.isAccessDenied

@Stable
data class PostKey(
    val authorId: String,
    val postId: String
)

@Stable
data class FeedItemUiState(
    val post: FeedPost,
    val author: UserProfile?,
    val likes: Set<String>,
    val isLoadingAuthor: Boolean = false,
    val authorError: DataFailure? = null,
    val isLoadingLikes: Boolean = false,
    val likesError: DataFailure? = null,
) {
    val key: PostKey get() = PostKey(post.authorId, post.id)
    val accessError: DataFailure? get() = authorError?.takeIf { it.isAccessDenied }
        ?: likesError?.takeIf { it.isAccessDenied }
    val isUnavailable: Boolean get() = accessError != null
}

@Stable
data class FeedUiState(
    val userId: String? = null,
    val posts: List<FeedItemUiState> = emptyList(),
    val isLoading: Boolean = true,
    val error: DataFailure? = null,
    val pendingLikes: Set<PostKey> = emptySet(),
    val likeErrors: Map<PostKey, DataFailure> = emptyMap(),
)
