@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.livesustainably.data.remote

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.livesustainably.domain.model.DataException
import com.livesustainably.domain.model.DataFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest

internal fun <T> Query.observeValue(map: (DataSnapshot) -> T): Flow<T> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            try {
                trySend(map(snapshot))
            } catch (error: Exception) {
                close(DataException(DataFailure.InvalidData, error))
            }
        }

        override fun onCancelled(error: DatabaseError) {
            close(DataException(when (error.code) {
                DatabaseError.PERMISSION_DENIED -> DataFailure.PermissionDenied
                DatabaseError.NETWORK_ERROR, DatabaseError.DISCONNECTED, DatabaseError.UNAVAILABLE -> DataFailure.Network
                else -> DataFailure.Unknown
            }, error.toException()))
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}.conflate()

internal fun <T> AuthRemoteDataSource.observeSignedIn(
    ownerId: String? = null,
    block: (String) -> Flow<T>,
): Flow<T> = userIds.flatMapLatest { userId ->
    if (userId == null) throw DataException(DataFailure.SignedOut)
    if (ownerId != null && userId != ownerId) throw DataException(DataFailure.PermissionDenied)
    block(userId)
}

internal fun AuthRemoteDataSource.requireOwner(userId: String) {
    val currentId = currentAccount?.userId ?: throw DataException(DataFailure.SignedOut)
    if (currentId != userId) throw DataException(DataFailure.PermissionDenied)
}

internal fun databaseKey(value: String): String {
    require(value.isNotEmpty() && value.toByteArray(Charsets.UTF_8).size <= 768 &&
        value.none { it in ".#$[]/" || it.code in 0..31 || it.code == 127 }) { "Invalid database key" }
    return value
}

internal suspend fun <T> databaseOperation(block: suspend () -> T): T = try {
    block()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: DataException) {
    throw error
} catch (error: Exception) {
    throw DataException(when {
        error.message.equals("Permission denied", ignoreCase = true) -> DataFailure.PermissionDenied
        error.message.equals("Client is offline", ignoreCase = true) -> DataFailure.Network
        else -> DataFailure.Unknown
    }, error)
}
