package com.livesustainably.ui.screens.feed

sealed interface FeedAction {
    data object Retry : FeedAction
    data class ToggleLike(val post: PostKey) : FeedAction
    data class RetryLike(val post: PostKey) : FeedAction
    data class RetryAuthor(val authorId: String) : FeedAction
    data class RetryLikes(val post: PostKey) : FeedAction
}
