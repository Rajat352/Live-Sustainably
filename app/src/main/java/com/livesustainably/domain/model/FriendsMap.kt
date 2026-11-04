package com.livesustainably.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun GeoPoint.distanceKmTo(other: GeoPoint): Double {
    val latitudeDelta = Math.toRadians(other.latitude - latitude)
    val longitudeDelta = Math.toRadians(other.longitude - longitude)
    val a = (sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)).coerceIn(0.0, 1.0)
    return 6371.0088 * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun nearbyDemoFriends(friends: List<DemoFriend>, configuration: DemoMapConfiguration): List<DemoFriend> =
    friends.distinctBy { it.profile.userId }
        .filter { configuration.center.distanceKmTo(it.position) <= configuration.radiusKm }
        .sortedWith(compareBy<DemoFriend> { configuration.center.distanceKmTo(it.position) }
            .thenBy { it.profile.userId })
