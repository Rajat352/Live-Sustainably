package com.livesustainably.data.remote

import kotlinx.coroutines.flow.Flow

data class AuthAccount(
    val userId: String,
    val email: String,
    val displayName: String?
)

interface AuthRemoteDataSource {
    val userIds: Flow<String?>
    val currentAccount: AuthAccount?
    suspend fun signIn(email: String, password: String)
    suspend fun createAccount(email: String, password: String)
    suspend fun updateDisplayName(displayName: String)
    fun signOut()
}
