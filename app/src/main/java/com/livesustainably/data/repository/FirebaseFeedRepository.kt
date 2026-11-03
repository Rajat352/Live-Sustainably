@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.livesustainably.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.livesustainably.data.remote.*
import com.livesustainably.domain.model.FeedPost
import com.livesustainably.domain.model.PostComment
import com.livesustainably.domain.repository.FeedRepository
import com.livesustainably.domain.repository.FriendsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await

class FirebaseFeedRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: AuthRemoteDataSource,
    private val friends: FriendsRepository,
) : FeedRepository {

    override fun observeFeed(userId: String) = friends.observeFriendIds(userId).flatMapLatest { ids ->
        combine((ids + userId).sorted().map { authorId ->
            database.getReference("postsByAuthor").child(databaseKey(authorId))
                .orderByChild("createdAt").limitToLast(30).observeValue { snapshot ->
                    snapshot.children.map {
                        requireNotNull(it.getValue(PostRecord::class.java)).toDomain(authorId, requireNotNull(it.key))
                    }
                }
        }) { posts -> mergeFeed(posts.toList()) }
    }

    override fun observeLikes(authorId: String, postId: String) = auth.observeSignedIn {
        postPath("postLikes", authorId, postId).observeValue { snapshot ->
            snapshot.children.map {
                require(it.getValue(Boolean::class.java) == true)
                requireNotNull(it.key)
            }.toSet()
        }
    }

    override fun observeComments(authorId: String, postId: String) = auth.observeSignedIn {
        postPath("postComments", authorId, postId).orderByChild("createdAt").limitToLast(100).observeValue { snapshot ->
            snapshot.children.map {
                requireNotNull(it.getValue(CommentRecord::class.java)).toDomain(requireNotNull(it.key))
            }.sortedWith(compareBy({ it.createdAtMillis }, { it.id }))
        }
    }

    override suspend fun setLiked(authorId: String, postId: String, userId: String, liked: Boolean) {
        auth.requireOwner(userId)
        databaseOperation {
            postPath("postLikes", authorId, postId).child(databaseKey(userId)).setValue(if (liked) true else null).await()
        }
    }

    override suspend fun addComment(authorId: String, postId: String, comment: PostComment) {
        auth.requireOwner(comment.authorId)
        require(comment.text.isNotBlank() && comment.text == comment.text.trim() && comment.text.length <= 500)
        require(comment.createdAtMillis >= 0)
        databaseOperation {
            // The caller retains this ID on retry, preventing duplicate comments
            postPath("postComments", authorId, postId).child(databaseKey(comment.id)).setValue(mapOf(
                "authorId" to comment.authorId,
                "text" to comment.text,
                "createdAt" to comment.createdAtMillis,
            )).await()
        }
    }

    private fun postPath(root: String, authorId: String, postId: String) =
        database.getReference(root).child(databaseKey(authorId)).child(databaseKey(postId))
}

internal fun mergeFeed(posts: List<List<FeedPost>>): List<FeedPost> = posts.flatten()
    .distinctBy { it.authorId to it.id }
    .sortedWith(compareByDescending<FeedPost> { it.createdAtMillis }.thenBy { it.authorId }.thenBy { it.id })
    .take(30)
