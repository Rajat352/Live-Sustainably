package com.livesustainably.domain.model

enum class ActivityCategory {
    Transport,
    Food,
    Shopping
}

data class SustainabilityActivity(
    val id: String,
    val createdAtMillis: Long,
    val category: ActivityCategory,
    val description: String,
    val savedKgCo2e: Double,
    val points: Int,
    val isDemo: Boolean,
)

data class UserProfile(
    val userId: String,
    val displayName: String,
    val avatarId: String
)

data class UserPreferences(
    val shareActivities: Boolean = true
)

data class FeedPost(
    val id: String,
    val authorId: String,
    val description: String,
    val createdAtMillis: Long,
    val savedKgCo2e: Double,
    val isDemo: Boolean,
)

data class PostComment(
    val id: String,
    val authorId: String,
    val text: String,
    val createdAtMillis: Long,
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

data class DemoFriend(
    val profile: UserProfile,
    val position: GeoPoint,
    val sampleStatus: String
)

data class DemoMapConfiguration(
    val center: GeoPoint,
    val radiusKm: Double
)

data class PeerScore(
    val userId: String,
    val points: Int,
    val isDemo: Boolean
)
