package com.livesustainably.ui.screens.comments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.livesustainably.domain.model.*
import com.livesustainably.domain.repository.AuthRepository
import com.livesustainably.domain.repository.FeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommentsViewModelTest {
    @Test
    fun `retry after timeout and recreation reuses the committed comment instead of duplicating it`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val stores = mutableListOf<ViewModelStore>()
        try {
            val auth = object : AuthRepository {
                override val session = MutableStateFlow<UserSession>(UserSession.SignedIn("reader"))
                override suspend fun signIn(email: String, password: String) = error("Unused")
                override suspend fun signUp(email: String, password: String, displayName: String) = error("Unused")
                override suspend fun signOut() = error("Unused")
                override suspend fun initializeUser(displayName: String?): UserProfile = error("Unused")
            }
            val feed = UncertainCommentRepository()
            val saved = SavedStateHandle()
            val thread = CommentThread("friend", "post")
            val original = CommentsViewModel(thread, auth, feed, saved)
            val originalStore = ViewModelStore().also { it.put("comments", original); stores += it }
            val subscription = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { original.state.collect() }
            runCurrent()

            original.updateDraft("  Great idea!  ")
            original.send()
            original.send()
            runCurrent()
            assertEquals(1, feed.attempts.size)
            assertTrue(original.state.value.isSending)

            // The server committed the write, but acknowledgement never arrived
            advanceTimeBy(15_000)
            runCurrent()
            assertEquals(DataFailure.Network, original.state.value.sendError)
            assertEquals(1, feed.comments.value.size)

            val restored = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
            subscription.cancel()
            originalStore.clear()
            val recreated = CommentsViewModel(thread, auth, feed, restored)
            ViewModelStore().also { it.put("comments", recreated); stores += it }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { recreated.state.collect() }
            runCurrent()
            assertEquals(feed.attempts.single().id, recreated.state.value.unconfirmedId)

            recreated.send()
            runCurrent()
            assertEquals(2, feed.attempts.size)
            assertEquals(feed.attempts.first(), feed.attempts.last())
            assertEquals("Great idea!", feed.comments.value.single().text)
            assertEquals("", recreated.state.value.draft)
            assertNull(recreated.state.value.unconfirmedId)
            assertNull(recreated.state.value.sendError)
            assertFalse(recreated.state.value.isSending)
        } finally {
            stores.forEach(ViewModelStore::clear)
            Dispatchers.resetMain()
        }
    }

    private class UncertainCommentRepository : FeedRepository {
        val attempts = mutableListOf<PostComment>()
        val comments = MutableStateFlow<List<PostComment>>(emptyList())
        override fun observeComments(authorId: String, postId: String) = comments
        override suspend fun addComment(authorId: String, postId: String, comment: PostComment) {
            assertEquals("friend", authorId)
            assertEquals("post", postId)
            attempts += comment
            comments.value = (comments.value.filterNot { it.id == comment.id } + comment)
            if (attempts.size == 1) awaitCancellation()
        }
        override fun observeFeed(userId: String) = flowOf(emptyList<FeedPost>())
        override fun observeLikes(authorId: String, postId: String) = flowOf(emptySet<String>())
        override suspend fun setLiked(authorId: String, postId: String, userId: String, liked: Boolean) = error("Unused")
    }
}
