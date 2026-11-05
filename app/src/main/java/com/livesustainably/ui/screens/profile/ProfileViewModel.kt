package com.livesustainably.ui.screens.profile

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesustainably.domain.model.AuthException
import com.livesustainably.domain.model.AuthFailure
import com.livesustainably.domain.model.DataException
import com.livesustainably.domain.model.DataFailure
import com.livesustainably.domain.model.PRESET_AVATARS
import com.livesustainably.domain.model.UserPreferences
import com.livesustainably.domain.model.UserProfile
import com.livesustainably.domain.model.UserSession
import com.livesustainably.domain.repository.AuthRepository
import com.livesustainably.domain.repository.ProfileRepository
import com.livesustainably.ui.common.dataFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()
    private var initialization: Job? = null
    private var observation: Job? = null
    private var profileWrite: Job? = null
    private var preferencesWrite: Job? = null
    private var signOutJob: Job? = null
    private var initialized = false

    init {

        viewModelScope.launch {
            combine(
                auth.session.filter { it != UserSession.Loading }
                    .map { (it as? UserSession.SignedIn)?.userId }.distinctUntilChanged(),
                _state.subscriptionCount.map { it > 0 }.distinctUntilChanged(),
            ) { userId, visible -> userId to visible }.collect { (userId, visible) ->
                observation?.cancelAndJoin()
                if (state.value.userId != userId) {
                    listOf(initialization, profileWrite, preferencesWrite, signOutJob).forEach { it?.cancelAndJoin() }
                    initialized = false
                    if (userId == null || savedState.get<String>("profileOwner") != userId) {
                        DRAFT_KEYS.forEach { savedState.remove<Any>(it) }
                    }
                    savedState["profileOwner"] = userId
                    _state.value = ProfileUiState(
                        userId = userId,
                        displayName = savedState["profileName"] ?: "",
                        avatarId = savedState.get<String>("profileAvatar")?.takeIf { it in PRESET_AVATARS } ?: "leaf",
                        shareActivities = savedState["profileSharing"] ?: true,
                        profileEdited = savedState["profileEdited"] ?: false,
                        preferencesEdited = savedState["preferencesEdited"] ?: false,
                    )
                    if (userId != null) initialize()
                }
                _state.update { it.copy(profile = null, preferences = null, isLoading = userId != null, loadError = null) }
                if (visible && initialized && !state.value.isSigningOut) observeProfile()
            }
        }
    }

    fun onAction(action: ProfileAction) {

        if (action == ProfileAction.SignOut) {
            signOut()
            return
        }

        if (state.value.userId == null || state.value.isSigningOut || _state.subscriptionCount.value == 0) return

        when (action) {

            ProfileAction.Retry -> if (!state.value.isInitializing) {
                if (!initialized) initialize() else observeProfile()
            }

            is ProfileAction.UpdateName -> editProfile { copy(displayName = action.value.take(51)) }

            is ProfileAction.SelectAvatar -> if (action.id in PRESET_AVATARS) editProfile { copy(avatarId = action.id) }

            is ProfileAction.ShareActivities -> if (state.value.canEdit && !state.value.isSavingPreferences) {
                _state.update { it.copy(shareActivities = action.enabled, preferencesEdited = true, preferencesSaved = false, preferencesSaveError = null) }
                saveDraft()
            }

            ProfileAction.SaveProfile -> saveProfile()

            ProfileAction.SavePreferences -> savePreferences()

            ProfileAction.DiscardProfile -> if (state.value.canEdit && !state.value.isSavingProfile) {
                val profile = state.value.profile ?: return
                _state.update { it.copy(displayName = profile.displayName, avatarId = profile.avatarId,
                    profileEdited = false, invalidName = false, profileSaveError = null, profileSaved = false) }
                saveDraft()
            }

            ProfileAction.DiscardPreferences -> if (state.value.canEdit && !state.value.isSavingPreferences) {
                _state.update { it.copy(shareActivities = requireNotNull(it.preferences).shareActivities,
                    preferencesEdited = false, preferencesSaveError = null, preferencesSaved = false) }
                saveDraft()
            }

            ProfileAction.SignOut -> Unit
        }
    }

    private fun initialize() {
        initialization?.cancel()
        _state.update { it.copy(isInitializing = true, initializationError = null) }
        initialization = viewModelScope.launch {
            try {
                confirmed { auth.initializeUser() }
                initialized = true
                _state.update { it.copy(isInitializing = false) }
                if (_state.subscriptionCount.value > 0 && !state.value.isSigningOut) observeProfile()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.update { it.copy(isInitializing = false, isLoading = false, initializationError = failure(e)) }
                Log.e(TAG, "An error occurred while initializing user", e)
            }
        }
    }

    private fun observeProfile() {
        observation?.cancel()
        _state.update { it.copy(profile = null, preferences = null, isLoading = true, loadError = null) }
        val userId = state.value.userId ?: return
        observation = viewModelScope.launch {
            try {
                combine(profiles.observeProfile(userId), profiles.observePreferences(userId)) { profile, preferences ->
                    (profile ?: throw DataException(DataFailure.InvalidData)) to preferences
                }.collect { (profile, preferences) ->
                    _state.update {
                        it.copy(profile = profile, preferences = preferences, isLoading = false, loadError = null,
                            displayName = if (it.profileEdited) it.displayName else profile.displayName,
                            avatarId = if (it.profileEdited) it.avatarId else profile.avatarId,
                            shareActivities = if (it.preferencesEdited) it.shareActivities else preferences.shareActivities)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.update { it.copy(profile = null, preferences = null, isLoading = false, loadError = failure(e)) }
                Log.e(TAG, "An error occurred while observing profile", e)
            }
        }
    }

    private fun editProfile(edit: ProfileUiState.() -> ProfileUiState) {
        if (!state.value.canEdit || state.value.isSavingProfile) return
        _state.update { it.edit().copy(profileEdited = true, invalidName = false, profileSaved = false, profileSaveError = null) }
        saveDraft()
    }

    private fun saveProfile() {
        val input = state.value
        if (!input.canEdit || input.isSavingProfile || !input.profileEdited) return
        val name = input.displayName.trim()
        if (name.length !in 1..50) {
            _state.update { it.copy(invalidName = true) }
            return
        }
        val profile = UserProfile(requireNotNull(input.userId), name, input.avatarId)
        _state.update { it.copy(isSavingProfile = true, profileSaveError = null, profileSaved = false) }
        profileWrite = viewModelScope.launch {
            try {
                confirmed { profiles.updateProfile(profile) }
                _state.update { it.copy(profile = profile, displayName = name, profileEdited = false,
                    isSavingProfile = false, profileSaved = true) }
                saveDraft()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.update { it.copy(isSavingProfile = false, profileSaveError = failure(e)) }
                Log.e(TAG, "An error occurred while saving profile", e)
            }
        }
    }

    private fun savePreferences() {
        val input = state.value
        if (!input.canEdit || input.isSavingPreferences || !input.preferencesEdited) return
        val preferences = UserPreferences(input.shareActivities)
        _state.update { it.copy(isSavingPreferences = true, preferencesSaveError = null, preferencesSaved = false) }
        preferencesWrite = viewModelScope.launch {
            try {
                confirmed { profiles.updatePreferences(requireNotNull(input.userId), preferences) }
                _state.update { it.copy(preferences = preferences, preferencesEdited = false,
                    isSavingPreferences = false, preferencesSaved = true) }
                saveDraft()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.update { it.copy(isSavingPreferences = false, preferencesSaveError = failure(e)) }
                Log.e(TAG, "An error occurred while saving preferences", e)
            }
        }
    }

    private fun signOut() {
        if (state.value.userId == null || state.value.isSigningOut) return
        listOf(initialization, observation, profileWrite, preferencesWrite).forEach { it?.cancel() }
        _state.update { it.copy(isSigningOut = true, isInitializing = false,
            isSavingProfile = false, isSavingPreferences = false, signOutError = null) }
        signOutJob = viewModelScope.launch {
            try {
                confirmed { auth.signOut() }
                DRAFT_KEYS.forEach { savedState.remove<Any>(it) }
                savedState.remove<String>("profileOwner")
                _state.value = ProfileUiState(isInitializing = false, isLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.update { it.copy(isSigningOut = false, signOutError = failure(e)) }
                Log.e(TAG, "An error occurred while signing out", e)
                if (!initialized) initialize() else if (_state.subscriptionCount.value > 0) observeProfile()
            }
        }
    }

    private fun saveDraft() {
        savedState["profileOwner"] = state.value.userId
        savedState["profileName"] = state.value.displayName
        savedState["profileAvatar"] = state.value.avatarId
        savedState["profileSharing"] = state.value.shareActivities
        savedState["profileEdited"] = state.value.profileEdited
        savedState["preferencesEdited"] = state.value.preferencesEdited
    }

    private suspend fun confirmed(write: suspend () -> Unit) {
        if (withTimeoutOrNull(15_000) { write(); true } != true) throw DataException(DataFailure.Network)
    }

    private fun failure(error: Exception): DataFailure = if (error is AuthException) when (error.reason) {
        AuthFailure.Network -> DataFailure.Network
        AuthFailure.PermissionDenied -> DataFailure.PermissionDenied
        AuthFailure.SignedOut -> DataFailure.SignedOut
        else -> DataFailure.Unknown
    } else dataFailure(error)

    private companion object {
        const val TAG = "ProfileViewModel"
        val DRAFT_KEYS = listOf("profileName", "profileAvatar", "profileSharing", "profileEdited", "preferencesEdited")
    }
}
