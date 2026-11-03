package com.livesustainably.domain.model

enum class AuthFailure {
    InvalidCredentials, EmailAlreadyUsed, InvalidEmail, WeakPassword, DisabledAccount,
    TooManyRequests, Network, ProviderDisabled, PermissionDenied, MissingDisplayName,
    SignedOut, Unknown,
}

class AuthException(val reason: AuthFailure, cause: Throwable? = null) : Exception(reason.name, cause)
