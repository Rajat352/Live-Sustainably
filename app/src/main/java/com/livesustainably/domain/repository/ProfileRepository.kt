package com.livesustainably.domain.repository

import com.livesustainably.domain.model.UserPreferences
import com.livesustainably.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(userId: String): Flow<UserProfile?>
    fun observePreferences(userId: String): Flow<UserPreferences>
    suspend fun updateProfile(profile: UserProfile)
    suspend fun updatePreferences(userId: String, preferences: UserPreferences)
}
