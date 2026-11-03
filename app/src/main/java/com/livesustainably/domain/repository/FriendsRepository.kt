package com.livesustainably.domain.repository

import com.livesustainably.domain.model.DemoFriend
import com.livesustainably.domain.model.DemoMapConfiguration
import kotlinx.coroutines.flow.Flow

interface FriendsRepository {
    fun observeFriendIds(userId: String): Flow<Set<String>>
    fun observeDemoFriends(userId: String): Flow<List<DemoFriend>>
    fun observeMapConfiguration(): Flow<DemoMapConfiguration>
}
