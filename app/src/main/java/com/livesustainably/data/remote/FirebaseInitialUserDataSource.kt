package com.livesustainably.data.remote

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.livesustainably.domain.model.AuthException
import com.livesustainably.domain.model.AuthFailure
import com.livesustainably.domain.model.UserProfile
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class FirebaseInitialUserDataSource @Inject constructor(
    private val database: FirebaseDatabase
) : InitialUserDataSource {

    override suspend fun ensureUser(account: AuthAccount): UserProfile {
        val profileRef = database.getReference("publicProfiles").child(account.userId)
        val existing = profileRef.get().await()
        val displayName = existing.child("displayName").getValue(String::class.java)
            ?: account.displayName?.trim()?.takeIf { it.length in 1..50 }
            ?: throw AuthException(AuthFailure.MissingDisplayName)

        database.getReference("privateUsers").child(account.userId)
            .createIfMissing(InitialUserRecords.privateUser(account))
        val profile = profileRef.createIfMissing(InitialUserRecords.publicProfile(displayName))
        return UserProfile(
            userId = account.userId,
            displayName = requireNotNull(profile.child("displayName").getValue(String::class.java)),
            avatarId = requireNotNull(profile.child("avatarId").getValue(String::class.java)),
        )
    }
}

private suspend fun DatabaseReference.createIfMissing(defaults: Map<String, Any>): DataSnapshot =
    suspendCancellableCoroutine { continuation ->
        runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                if (currentData.value == null) currentData.value = defaults
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (!continuation.isActive) return
                when {
                    error != null -> continuation.resumeWithException(AuthException(
                        if (error.code == DatabaseError.PERMISSION_DENIED) AuthFailure.PermissionDenied else AuthFailure.Network,
                        error.toException(),
                    ))
                    committed && currentData != null -> continuation.resume(currentData)
                    else -> continuation.resumeWithException(AuthException(AuthFailure.Unknown))
                }
            }
        }, false)
    }
