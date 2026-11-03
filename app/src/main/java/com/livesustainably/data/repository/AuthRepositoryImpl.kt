package com.livesustainably.data.repository

import com.livesustainably.data.remote.AuthRemoteDataSource
import com.livesustainably.data.remote.InitialUserDataSource
import com.livesustainably.domain.model.AuthException
import com.livesustainably.domain.model.AuthFailure
import com.livesustainably.domain.model.UserProfile
import com.livesustainably.domain.model.UserSession
import com.livesustainably.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: AuthRemoteDataSource,
    private val users: InitialUserDataSource,
) : AuthRepository {

    private val mutex = Mutex()

    override val session = auth.userIds.map { userId ->
        mutex.withLock {
            userId?.let(UserSession::SignedIn) ?: UserSession.SignedOut
        }
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String) = authOperation {
        mutex.withLock { auth.signIn(email.trim(), password) }
    }

    override suspend fun signUp(email: String, password: String, displayName: String) = authOperation {
        mutex.withLock {
            auth.createAccount(email.trim(), password)
            auth.updateDisplayName(displayName.trim())
        }
    }

    override suspend fun signOut() = authOperation { mutex.withLock { auth.signOut() } }

    override suspend fun initializeUser(displayName: String?): UserProfile = authOperation {
        val account = mutex.withLock {
            if (displayName != null) {
                val trimmed = displayName.trim()
                if (trimmed.length !in 1..50) throw AuthException(AuthFailure.MissingDisplayName)
                auth.updateDisplayName(trimmed)
            }
            auth.currentAccount ?: throw AuthException(AuthFailure.SignedOut)
        }

        withTimeoutOrNull(15_000) { users.ensureUser(account) }
            ?: throw AuthException(AuthFailure.Network)
    }
}
