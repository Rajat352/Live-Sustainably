package com.livesustainably.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesustainably.domain.model.UserProfile
import com.livesustainably.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val profile: UserProfile? = null,
    val isLoadingProfile: Boolean = true,
    val isSigningOut: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    private var initialization: Job? = null

    init {
        loadProfile()
    }

    fun retryProfile() {
        if (!_state.value.isLoadingProfile && !_state.value.isSigningOut) loadProfile()
    }

    private fun loadProfile() {
        _state.update { it.copy(isLoadingProfile = true, error = null) }

        initialization = viewModelScope.launch {
            try {
                val profile = repository.initializeUser()
                _state.update { it.copy(profile = profile, isLoadingProfile = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingProfile = false,
                        error = "An error occurred while loading profile",
                    )
                }
                Log.e(TAG, "An error occurred while loading profile", e)
            }
        }
    }

    fun signOut() {
        if (_state.value.isSigningOut) return

        initialization?.cancel()
        _state.update { it.copy(isSigningOut = true, isLoadingProfile = false, error = null) }
        viewModelScope.launch {
            try {
                repository.signOut()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.update { it.copy(isSigningOut = false, error = "An error occurred while signing out") }
                Log.e(TAG, "An error occurred while signing out", e)
            }
        }
    }

    companion object {
        const val TAG = "ProfileViewModel"
    }
}
