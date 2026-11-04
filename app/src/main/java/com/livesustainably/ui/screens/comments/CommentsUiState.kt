package com.livesustainably.ui.screens.comments

import androidx.compose.runtime.Stable
import com.livesustainably.domain.model.PostComment
import com.livesustainably.domain.model.DataFailure

data class CommentThread(val authorId: String, val postId: String)

@Stable
data class CommentsUiState(
    val userId: String? = null,
    val comments: List<PostComment> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: DataFailure? = null,
    val draft: String = "",
    val draftError: Boolean = false,
    val isSending: Boolean = false,
    val unconfirmedId: String? = null,
    val sendError: DataFailure? = null,
)
