package com.livesustainably.ui.screens.auth

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesustainably.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val displayNameError: String? = null,
    val confirmPasswordError: String? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthFormState())
    val state = _state.asStateFlow()

    fun updateEmail(value: String) = edit { copy(email = value, emailError = null) }
    fun updatePassword(value: String) = edit { copy(password = value, passwordError = null) }
    fun updateDisplayName(value: String) = edit { copy(displayName = value, displayNameError = null) }
    fun updateConfirmPassword(value: String) = edit { copy(confirmPassword = value, confirmPasswordError = null) }

    private fun edit(change: AuthFormState.() -> AuthFormState) {
        if (!_state.value.isSubmitting) _state.update { it.change().copy(error = null) }
    }

    fun submit(isSignup: Boolean) {
        val input = _state.value
        if (input.isSubmitting) return

        // Validation
        val validated = input.copy(
            emailError = if (input.email.trim().length > 254 || !EMAIL.matches(input.email.trim())) "Enter a valid email address." else null,
            passwordError = when {
                input.password.isEmpty() -> "Enter your password."
                isSignup && input.password.length < 6 -> "Use at least 6 characters."
                input.password.length > 4096 -> "Use no more than 4096 characters."
                else -> null
            },
            displayNameError = if (isSignup && input.displayName.trim().length !in 1..50) "Use a display name of 1–50 characters." else null,
            confirmPasswordError = if (isSignup && input.confirmPassword != input.password) "Passwords do not match." else null,
            error = null,
        )

        _state.value = validated
        if (listOf(validated.emailError, validated.passwordError, validated.displayNameError, validated.confirmPasswordError).any { it != null }) return

        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                if (isSignup) {
                    repository.signUp(input.email.trim(), input.password, input.displayName.trim())
                } else {
                    repository.signIn(input.email.trim(), input.password)
                }

                _state.update { it.copy(password = "", confirmPassword = "", isSubmitting = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false, error = "An error occurred while submitting, please check your email or password") }
                Log.e(TAG, "An error occurred while submitting", e)
            }
        }
    }

    private companion object {
        const val TAG = "AuthViewModel"
        val EMAIL = Regex("^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+$")
    }
}
