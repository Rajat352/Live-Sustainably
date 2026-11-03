package com.livesustainably.domain.repository

import com.livesustainably.domain.model.FeedPost
import com.livesustainably.domain.model.PostComment
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun observeFeed(userId: String): Flow<List<FeedPost>>
    fun observeLikes(authorId: String, postId: String): Flow<Set<String>>
    fun observeComments(authorId: String, postId: String): Flow<List<PostComment>>
    suspend fun setLiked(authorId: String, postId: String, userId: String, liked: Boolean)
    suspend fun addComment(authorId: String, postId: String, comment: PostComment)
}
