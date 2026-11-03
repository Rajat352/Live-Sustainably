@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.livesustainably.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.livesustainably.data.remote.*
import com.livesustainably.domain.model.DataException
import com.livesustainably.domain.model.DataFailure
import com.livesustainably.domain.model.DemoFriend
import com.livesustainably.domain.repository.FriendsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class FirebaseFriendsRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: AuthRemoteDataSource,
) : FriendsRepository {

    override fun observeFriendIds(userId: String) = auth.observeSignedIn(userId) {
        database.getReference("friendships").child(databaseKey(userId)).observeValue { snapshot ->
            snapshot.children.map { child ->
                require(child.getValue(Boolean::class.java) == true)
                databaseKey(requireNotNull(child.key))
            }.toSet()
        }
    }

    override fun observeDemoFriends(userId: String) = observeFriendIds(userId).flatMapLatest { ids ->
        if (ids.isEmpty()) flowOf(emptyList()) else combine(ids.sorted().map { id ->
            // A real friend has no demo record; never invent a location for them.
            database.getReference("demoFriends").child(id).observeValue {
                it.getValue(DemoFriendRecord::class.java)
            }.flatMapLatest { location ->
                if (location == null) flowOf<DemoFriend?>(null) else
                    database.getReference("publicProfiles").child(id).observeValue {
                        location.toDomain(requireNotNull(it.getValue(ProfileRecord::class.java)).toDomain(id))
                    }
            }
        }) { friends -> friends.filterNotNull() }
    }

    override fun observeMapConfiguration() = auth.observeSignedIn {
        database.getReference("mapConfig/demoCenter").observeValue {
            requireNotNull(it.getValue(MapConfigurationRecord::class.java)).toDomain()
        }
    }

    override suspend fun provisionDemoFriends(userId: String) {
        auth.requireOwner(userId)
        databaseOperation {
            val catalog = database.getReference("demoFriendCatalog").get().await()
            val friends = catalog.children.associate { child ->
                require(child.getValue(Boolean::class.java) == true)
                databaseKey(requireNotNull(child.key)) to true
            }
            if (friends.isEmpty()) throw DataException(DataFailure.DemoNotConfigured)
            // Merge only trusted demo IDs. Repeating initialization preserves admin-assigned real friends.
            auth.requireOwner(userId)
            database.getReference("friendships").child(databaseKey(userId)).updateChildren(friends).await()
        }
    }
}
