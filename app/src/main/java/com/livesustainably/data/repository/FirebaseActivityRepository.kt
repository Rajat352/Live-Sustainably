@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.livesustainably.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.livesustainably.data.remote.*
import com.livesustainably.domain.model.PeerScore
import com.livesustainably.domain.repository.ActivityRepository
import com.livesustainably.domain.repository.FriendsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class FirebaseActivityRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: AuthRemoteDataSource,
    private val friends: FriendsRepository,
) : ActivityRepository {

    override fun observeActivities(userId: String, startMillis: Long, endMillis: Long) = auth.observeSignedIn(userId) {
        require(startMillis >= 0 && endMillis > startMillis)
        database.getReference("activities").child(databaseKey(userId)).orderByChild("createdAt")
            .startAt(startMillis.toDouble()).endBefore(endMillis.toDouble()).observeValue { snapshot ->
                snapshot.children.map {
                    requireNotNull(it.getValue(ActivityRecord::class.java)).toDomain(requireNotNull(it.key))
                }.sortedWith(compareBy({ it.createdAtMillis }, { it.id }))
            }
    }

    override fun observePeerScores(periodKey: String) = auth.observeSignedIn { userId ->
        require(Regex("(?:week_\\d{4}-\\d{2}-\\d{2}|month_\\d{4}-\\d{2})").matches(periodKey))
        friends.observeFriendIds(userId).flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList()) else combine(ids.sorted().map { id ->
                database.getReference("leaderboards").child(periodKey).child(id).observeValue {
                    it.getValue(PeerScoreRecord::class.java)?.toDomain(id)
                }
            }) { scores -> scores.filterNotNull().sortedWith(compareByDescending<PeerScore> { it.points }.thenBy { it.userId }) }
        }
    }
}
