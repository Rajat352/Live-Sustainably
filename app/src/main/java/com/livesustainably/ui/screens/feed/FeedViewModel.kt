@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.livesustainably.ui.screens.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesustainably.domain.model.UserSession
import com.livesustainably.domain.model.DataException
import com.livesustainably.domain.model.DataFailure
import com.livesustainably.domain.repository.AuthRepository
import com.livesustainably.domain.repository.FeedRepository
import com.livesustainably.domain.repository.ProfileRepository
import com.livesustainably.ui.common.dataFailure
import com.livesustainably.ui.common.isAccessDenied
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class FeedViewModel @Inject constructor(
    auth: AuthRepository,
    private val feed: FeedRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(FeedUiState())
    val state = _state.asStateFlow()
    private var observation: Job? = null
    private val writes = mutableMapOf<PostKey, Job>()
    private val failedLikes = mutableMapOf<PostKey, Boolean>()
    private val authorRetries = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val likesRetries = MutableStateFlow<Map<PostKey, Int>>(emptyMap())

    init {
        viewModelScope.launch {
            // Account cleanup stays active; only database reads depend on screen subscribers.
            combine(
                auth.session.filter { it != UserSession.Loading }
                    .map { (it as? UserSession.SignedIn)?.userId }.distinctUntilChanged(),
                _state.subscriptionCount.map { it > 0 }.distinctUntilChanged(),
            ) { userId, visible -> userId to visible }.collect { (userId, visible) ->
                observation?.cancelAndJoin()
                observation = null
                if (_state.value.userId != userId) {
                    writes.values.toList().forEach { it.cancelAndJoin() }
                    writes.clear()
                    failedLikes.clear()
                    authorRetries.value = emptyMap()
                    likesRetries.value = emptyMap()
                    _state.value = FeedUiState(userId = userId, isLoading = userId != null)
                }
                // Revalidate content on return; do not replay possibly revoked access from a hidden tab.
                _state.update { it.copy(posts = emptyList(), isLoading = userId != null, error = null) }
                if (visible && userId != null) loadFeed(userId)
            }
        }
    }

    fun onAction(action: FeedAction) {
        val userId = state.value.userId ?: return
        if (_state.subscriptionCount.value == 0) return
        when (action) {
            FeedAction.Retry -> if (!state.value.isLoading) loadFeed(userId)
            is FeedAction.ToggleLike -> {
                val item = state.value.posts.find { it.key == action.post } ?: return
                if (!item.isLoadingLikes && item.likesError == null) setLiked(action.post, userId, userId !in item.likes)
            }
            is FeedAction.RetryLike -> failedLikes[action.post]?.let { setLiked(action.post, userId, it) }
            is FeedAction.RetryAuthor -> authorRetries.update { it + (action.authorId to ((it[action.authorId] ?: 0) + 1)) }
            is FeedAction.RetryLikes -> likesRetries.update { it + (action.post to ((it[action.post] ?: 0) + 1)) }
        }
    }

    private fun loadFeed(userId: String) {
        observation?.cancel()
        _state.update { it.copy(isLoading = true, posts = emptyList(), error = null) }
        observation = viewModelScope.launch {
            try {
                feed.observeFeed(userId).flatMapLatest { posts ->
                    val keys = posts.map { PostKey(it.authorId, it.id) }.toSet()
                    val authorIds = posts.map { it.authorId }.toSet()
                    failedLikes.keys.retainAll(keys)
                    authorRetries.update { it.filterKeys(authorIds::contains) }
                    likesRetries.update { it.filterKeys(keys::contains) }
                    if (posts.isEmpty()) flowOf(emptyList()) else {
                        val authors = combine(authorIds.map { id ->
                            observeDetail(authorRetries.map { it[id] ?: 0 }.distinctUntilChanged()) {
                                profiles.observeProfile(id)
                            }.map { id to it }
                        }) { it.toMap() }
                        val likes = combine(posts.map { post ->
                            val key = PostKey(post.authorId, post.id)
                            observeDetail(likesRetries.map { it[key] ?: 0 }.distinctUntilChanged()) {
                                feed.observeLikes(post.authorId, post.id)
                            }
                        }) { it.toList() }
                        combine(authors, likes) { people, reactions ->
                            posts.mapIndexed { index, post ->
                                val author = people.getValue(post.authorId)
                                val reaction = reactions[index]
                                FeedItemUiState(
                                    post = post, author = author.value, likes = reaction.value.orEmpty(),
                                    isLoadingAuthor = author.isLoading, authorError = author.error,
                                    isLoadingLikes = reaction.isLoading, likesError = reaction.error,
                                )
                            }
                        }
                    }
                }.collect { posts ->
                    val keys = posts.map { it.key }.toSet()
                    _state.update { it.copy(posts = posts, isLoading = false, error = null, likeErrors = it.likeErrors.filterKeys(keys::contains)) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.update { it.copy(posts = emptyList(), isLoading = false, error = dataFailure(e)) }
                Log.e(TAG, "An error occurred while loading feed", e)
            }
        }
    }

    private fun <T> observeDetail(retries: Flow<Int>, read: () -> Flow<T>): Flow<FeedDetail<T>> = flow {
        var previousFailure: DataFailure? = null
        emitAll(retries.flatMapLatest {
            flow {
                emit(FeedDetail<T>(error = previousFailure?.takeIf { it.isAccessDenied }))
                emitAll(read().map {
                    previousFailure = null
                    FeedDetail(value = it, isLoading = false)
                })
            }.catch { error ->
                if (error is CancellationException) throw error
                previousFailure = dataFailure(error)
                emit(FeedDetail(isLoading = false, error = previousFailure))
                Log.e(TAG, "An error occurred while loading feed details", error)
            }
        })
    }

    private fun setLiked(key: PostKey, userId: String, liked: Boolean) {
        val item = state.value.posts.find { it.key == key } ?: return
        if (key in state.value.pendingLikes || item.isUnavailable) return
        _state.update { it.copy(pendingLikes = it.pendingLikes + key, likeErrors = it.likeErrors - key) }
        writes[key] = viewModelScope.launch {
            try {
                val confirmed = withTimeoutOrNull(15_000) {
                    feed.setLiked(key.authorId, key.postId, userId, liked)
                    true
                } ?: false
                if (!confirmed) throw DataException(DataFailure.Network)
                failedLikes.remove(key)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                failedLikes[key] = liked
                _state.update { it.copy(likeErrors = it.likeErrors + (key to dataFailure(e))) }
                Log.e(TAG, "An error occurred while posting a like", e)
            } finally {
                _state.update { it.copy(pendingLikes = it.pendingLikes - key) }
                writes.remove(key)
            }
        }
    }

    companion object {
        const val TAG = "FeedViewModel"
    }
}

private data class FeedDetail<T>(
    val value: T? = null,
    val isLoading: Boolean = true,
    val error: DataFailure? = null,
)
