package com.livesustainably.domain.repository

import com.livesustainably.domain.model.PeerScore
import com.livesustainably.domain.model.SustainabilityActivity
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    /** Inclusive start, exclusive end; timestamps are epoch milliseconds. */
    fun observeActivities(userId: String, startMillis: Long, endMillis: Long): Flow<List<SustainabilityActivity>>
    fun observePeerScores(periodKey: String): Flow<List<PeerScore>>
}
