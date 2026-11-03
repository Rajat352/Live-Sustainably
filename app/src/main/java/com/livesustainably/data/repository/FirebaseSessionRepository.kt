package com.livesustainably.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.livesustainably.domain.model.UserSession
import com.livesustainably.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class FirebaseSessionRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : SessionRepository {
    override val session: Flow<UserSession> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val state = firebaseAuth.currentUser?.uid?.let(UserSession::SignedIn)
                ?: UserSession.SignedOut
            trySend(state)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()
}
