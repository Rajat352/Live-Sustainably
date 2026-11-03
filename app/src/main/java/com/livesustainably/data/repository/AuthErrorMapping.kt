package com.livesustainably.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.livesustainably.domain.model.AuthException
import com.livesustainably.domain.model.AuthFailure
import kotlinx.coroutines.CancellationException

internal suspend fun <T> authOperation(block: suspend () -> T): T = try {
    block()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: AuthException) {
    throw error
} catch (error: Exception) {
    val reason = when {
        error is FirebaseNetworkException -> AuthFailure.Network
        error is FirebaseTooManyRequestsException -> AuthFailure.TooManyRequests
        error is FirebaseAuthException -> authFailureForCode(error.errorCode)
        // RTDB get() wraps DatabaseError in a plain Exception rather than DatabaseException.
        error.message.equals("Permission denied", ignoreCase = true) -> AuthFailure.PermissionDenied
        error.message.equals("Client is offline", ignoreCase = true) -> AuthFailure.Network
        else -> AuthFailure.Unknown
    }
    throw AuthException(reason, error)
}

internal fun authFailureForCode(code: String): AuthFailure = when (code) {
    "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> AuthFailure.InvalidCredentials
    "ERROR_EMAIL_ALREADY_IN_USE" -> AuthFailure.EmailAlreadyUsed
    "ERROR_INVALID_EMAIL" -> AuthFailure.InvalidEmail
    "ERROR_WEAK_PASSWORD", "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS" -> AuthFailure.WeakPassword
    "ERROR_USER_DISABLED" -> AuthFailure.DisabledAccount
    "ERROR_TOO_MANY_REQUESTS" -> AuthFailure.TooManyRequests
    "ERROR_NETWORK_REQUEST_FAILED" -> AuthFailure.Network
    "ERROR_OPERATION_NOT_ALLOWED" -> AuthFailure.ProviderDisabled
    else -> AuthFailure.Unknown
}
