package com.livesustainably.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.livesustainably.domain.model.AuthException
import com.livesustainably.domain.model.AuthFailure
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class FirebaseAuthRemoteDataSource @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRemoteDataSource {

    override val userIds = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override val currentAccount: AuthAccount?
        get() = auth.currentUser?.let {
            AuthAccount(
                it.uid,
                it.email.orEmpty(),
                it.displayName
            )
        }

    override suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    override suspend fun updateDisplayName(displayName: String) {
        val user = auth.currentUser ?: throw AuthException(AuthFailure.SignedOut)
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build()).await()
    }

    override fun signOut() = auth.signOut()
}
