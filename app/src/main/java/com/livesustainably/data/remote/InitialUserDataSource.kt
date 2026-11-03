package com.livesustainably.data.remote

import com.livesustainably.domain.model.UserProfile

interface InitialUserDataSource {
    suspend fun ensureUser(account: AuthAccount): UserProfile
}

internal object InitialUserRecords {
    fun privateUser(account: AuthAccount): Map<String, Any> = mapOf(
        "email" to account.email,
        "preferences" to mapOf("shareActivities" to true),
    )

    fun publicProfile(displayName: String): Map<String, Any> = mapOf(
        "displayName" to displayName,
        "avatarId" to "leaf",
    )
}
