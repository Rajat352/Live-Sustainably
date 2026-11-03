package com.livesustainably.domain.model

sealed interface UserSession {
    data object Loading : UserSession
    data object SignedOut : UserSession
    data class SignedIn(val userId: String) : UserSession
}
