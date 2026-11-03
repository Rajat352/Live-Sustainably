package com.livesustainably.data.remote

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import com.livesustainably.domain.model.*

@IgnoreExtraProperties
internal data class ProfileRecord(val displayName: String? = null, val avatarId: String? = null) {
    fun toDomain(userId: String) = UserProfile(
        userId,
        requireNotNull(displayName).also {
            require(it.isNotBlank() && it.length <= 50)
        },
        requireNotNull(avatarId).also { require(it in PRESET_AVATARS) })
}

@IgnoreExtraProperties
internal data class PreferencesRecord(val shareActivities: Boolean? = null) {
    fun toDomain() = UserPreferences(requireNotNull(shareActivities))
}

@IgnoreExtraProperties
internal data class ActivityRecord(
    val createdAt: Long? = null,
    val category: String? = null,
    val description: String? = null,
    val savedKgCo2e: Double? = null,
    val points: Int? = null,
    @get:PropertyName("isDemo") @field:PropertyName("isDemo") val isDemo: Boolean? = null,
) {
    fun toDomain(id: String) = SustainabilityActivity(
        id,
        timestamp(createdAt),
        ActivityCategory.valueOf(requireNotNull(category)),
        description(description),
        saving(savedKgCo2e),
        score(points),
        requireNotNull(isDemo),
    )
}

@IgnoreExtraProperties
internal data class PostRecord(
    val authorId: String? = null,
    val description: String? = null,
    val createdAt: Long? = null,
    val savedKgCo2e: Double? = null,
    @get:PropertyName("isDemo") @field:PropertyName("isDemo") val isDemo: Boolean? = null,
) {
    fun toDomain(author: String, id: String): FeedPost {
        require(authorId == author)
        return FeedPost(
            id,
            author,
            description(description),
            timestamp(createdAt),
            saving(savedKgCo2e),
            requireNotNull(isDemo))
    }
}

@IgnoreExtraProperties
internal data class CommentRecord(
    val authorId: String? = null,
    val text: String? = null,
    val createdAt: Long? = null,
) {
    fun toDomain(id: String) = PostComment(
        id,
        databaseKey(requireNotNull(authorId)), requireNotNull(text).also {
            require(it.isNotBlank() && it.length <= 500)
        },
        timestamp(createdAt)
    )
}

@IgnoreExtraProperties
internal data class DemoFriendRecord(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sampleStatus: String? = null,
    @get:PropertyName("isDemo") @field:PropertyName("isDemo") val isDemo: Boolean? = null,
) {
    fun toDomain(profile: UserProfile): DemoFriend {
        require(isDemo == true)
        return DemoFriend(
            profile,
            point(latitude, longitude),
            requireNotNull(sampleStatus).also {
                require(it.isNotBlank() && it.length <= 100)
            })
    }
}

@IgnoreExtraProperties
internal data class MapConfigurationRecord(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Double? = null,
) {
    fun toDomain() = DemoMapConfiguration(
        point(latitude, longitude),
        requireNotNull(radiusKm).also {
            require(it.isFinite() && it > 0 && it <= 100)
        })
}

@IgnoreExtraProperties
internal data class PeerScoreRecord(
    val points: Int? = null,
    @get:PropertyName("isDemo") @field:PropertyName("isDemo") val isDemo: Boolean? = null,
) {
    fun toDomain(userId: String) = PeerScore(userId, score(points), requireNotNull(isDemo))
}

internal fun UserProfile.toRecord(): Map<String, Any> {
    require(displayName.isNotBlank() && displayName == displayName.trim() && displayName.length <= 50)
    require(avatarId in PRESET_AVATARS)
    return mapOf("displayName" to displayName, "avatarId" to avatarId)
}

private fun timestamp(value: Long?) = requireNotNull(value).also { require(it >= 0) }
private fun description(value: String?) = requireNotNull(value).also { require(it.isNotBlank() && it.length <= 280) }
private fun saving(value: Double?) = requireNotNull(value).also { require(it.isFinite() && it in 0.0..1_000_000.0) }
private fun score(value: Int?) = requireNotNull(value).also { require(it in 0..1_000_000) }
private fun point(latitude: Double?, longitude: Double?) = GeoPoint(
    requireNotNull(latitude).also { require(it.isFinite() && it in -90.0..90.0) },
    requireNotNull(longitude).also { require(it.isFinite() && it in -180.0..180.0) },
)
