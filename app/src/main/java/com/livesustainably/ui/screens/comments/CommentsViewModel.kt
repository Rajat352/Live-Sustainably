package com.livesustainably.ui.screens.comments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesustainably.domain.model.PostComment
import com.livesustainably.domain.model.DataException
import com.livesustainably.domain.model.DataFailure
import com.livesustainably.domain.model.UserSession
import com.livesustainably.domain.repository.AuthRepository
import com.livesustainably.domain.repository.FeedRepository
import com.livesustainably.ui.common.dataFailure
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel(assistedFactory = CommentsViewModel.Factory::class)
class CommentsViewModel @AssistedInject constructor(
    @Assisted private val thread: CommentThread,
    auth: AuthRepository,
    private val feed: FeedRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(CommentsUiState())
    val state = _state.asStateFlow()
    private var observation: Job? = null
    private var submission: Job? = null
    private var pendingComment: PostComment? = null

    init {
        viewModelScope.launch {
            combine(
                auth.session.filter { it != UserSession.Loading }
                    .map { (it as? UserSession.SignedIn)?.userId }.distinctUntilChanged(),
                _state.subscriptionCount.map { it > 0 }.distinctUntilChanged(),
            ) { userId, visible -> userId to visible }.collect { (userId, visible) ->
                observation?.cancelAndJoin()
                observation = null
                if (_state.value.userId != userId) {
                    submission?.cancelAndJoin()
                    // A session change must never carry another account's draft or pending write forward.
                    if (savedState.get<String>("draftOwner") != userId) clearSavedDraft()
                    savedState["draftOwner"] = userId
                    pendingComment = restorePending(userId)
                    _state.value = CommentsUiState(
                        userId = userId,
                        isLoading = userId != null,
                        draft = savedState["draft"] ?: "",
                        unconfirmedId = pendingComment?.id,
                        sendError = pendingComment?.let { DataFailure.Unknown },
                    )
                }
                _state.update { it.copy(comments = emptyList(), isLoading = userId != null, loadError = null) }
                if (visible && userId != null) loadComments()
            }
        }
    }

    fun updateDraft(value: String) {
        if (state.value.isSending || state.value.userId == null) return
        val bounded = value.take(501)
        if (bounded.trim() != pendingComment?.text) clearPending()
        savedState["draft"] = bounded
        _state.update { it.copy(draft = bounded, draftError = false, sendError = null, unconfirmedId = pendingComment?.id) }
    }

    fun retry() {
        if (!state.value.isLoading && state.value.userId != null && _state.subscriptionCount.value > 0) loadComments()
    }

    private fun loadComments() {
        observation?.cancel()
        _state.update { it.copy(isLoading = true, loadError = null, comments = emptyList()) }
        observation = viewModelScope.launch {
            try {
                feed.observeComments(thread.authorId, thread.postId).collect { comments ->
                    _state.update { it.copy(comments = comments, isLoading = false, loadError = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(comments = emptyList(), isLoading = false, loadError = dataFailure(error)) }
            }
        }
    }

    fun send() {
        val input = state.value
        val userId = input.userId ?: return
        if (input.isSending || input.isLoading || input.loadError != null || _state.subscriptionCount.value == 0) return
        val text = input.draft.trim()
        if (text.isBlank() || text.length > 500) {
            _state.update { it.copy(draftError = true) }
            return
        }
        val comment = pendingComment?.takeIf { it.text == text && it.authorId == userId }
            ?: PostComment(UUID.randomUUID().toString(), userId, text, System.currentTimeMillis())
        pendingComment = comment
        savedState["pendingId"] = comment.id
        savedState["pendingText"] = comment.text
        savedState["pendingCreatedAt"] = comment.createdAtMillis
        _state.update { it.copy(isSending = true, draftError = false, sendError = null, unconfirmedId = comment.id) }
        submission = viewModelScope.launch {
            try {
                val confirmed = withTimeoutOrNull(15_000) {
                    feed.addComment(thread.authorId, thread.postId, comment)
                    true
                } ?: false
                if (!confirmed) throw DataException(DataFailure.Network)
                clearPending()
                savedState["draft"] = ""
                _state.update { it.copy(isSending = false, draft = "", unconfirmedId = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(isSending = false, sendError = dataFailure(error)) }
            }
        }
    }

    private fun restorePending(userId: String?): PostComment? {
        val id = savedState.get<String>("pendingId") ?: return null
        val text = savedState.get<String>("pendingText") ?: return null
        val createdAt = savedState.get<Long>("pendingCreatedAt") ?: return null
        return userId?.let { PostComment(id, it, text, createdAt) }
    }

    private fun clearPending() {
        pendingComment = null
        savedState.remove<String>("pendingId")
        savedState.remove<String>("pendingText")
        savedState.remove<Long>("pendingCreatedAt")
    }

    private fun clearSavedDraft() {
        clearPending()
        savedState["draft"] = ""
    }

    @AssistedFactory
    interface Factory {
        fun create(thread: CommentThread): CommentsViewModel
    }
}
