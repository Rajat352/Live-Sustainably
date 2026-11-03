package com.livesustainably.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.livesustainably.data.remote.*
import com.livesustainably.domain.model.UserPreferences
import com.livesustainably.domain.model.UserProfile
import com.livesustainably.domain.repository.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class FirebaseProfileRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: AuthRemoteDataSource,
) : ProfileRepository {

    override fun observeProfile(userId: String) = auth.observeSignedIn {
        database.getReference("publicProfiles").child(databaseKey(userId)).observeValue {
            it.getValue(ProfileRecord::class.java)?.toDomain(userId)
        }
    }

    override fun observePreferences(userId: String) = auth.observeSignedIn(userId) {
        database.getReference("privateUsers").child(databaseKey(userId)).child("preferences").observeValue {
            requireNotNull(it.getValue(PreferencesRecord::class.java)).toDomain()
        }
    }

    override suspend fun updateProfile(profile: UserProfile) {
        auth.requireOwner(profile.userId)
        val record = profile.toRecord()
        databaseOperation {
            database.getReference("publicProfiles").child(databaseKey(profile.userId)).setValue(record).await()
        }
    }

    override suspend fun updatePreferences(userId: String, preferences: UserPreferences) {
        auth.requireOwner(userId)
        databaseOperation {
            database.getReference("privateUsers").child(databaseKey(userId)).child("preferences")
                .setValue(mapOf("shareActivities" to preferences.shareActivities)).await()
        }
    }
}
