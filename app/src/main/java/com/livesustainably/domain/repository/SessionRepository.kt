package com.livesustainably.domain.repository

import com.livesustainably.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/** Session observation is available before the authentication forms are implemented. */
interface SessionRepository {
    val session: Flow<UserSession>
}
