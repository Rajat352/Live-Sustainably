package com.livesustainably.domain.repository

interface AuthRepository : SessionRepository {
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String, displayName: String)
    suspend fun signOut()
}
