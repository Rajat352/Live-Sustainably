package com.livesustainably.domain.repository

import com.livesustainably.domain.model.UserProfile
import com.livesustainably.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<UserSession>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String, displayName: String)
    suspend fun signOut()
    suspend fun initializeUser(displayName: String? = null): UserProfile
}
