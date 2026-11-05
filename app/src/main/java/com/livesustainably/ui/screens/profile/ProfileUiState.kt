package com.livesustainably.ui.screens.profile

import androidx.compose.runtime.Stable
import com.livesustainably.domain.model.DataFailure
import com.livesustainably.domain.model.UserPreferences
import com.livesustainably.domain.model.UserProfile

@Stable
data class ProfileUiState(
    val userId: String? = null,
    val profile: UserProfile? = null,
    val preferences: UserPreferences? = null,
    val displayName: String = "",
    val avatarId: String = "leaf",
    val shareActivities: Boolean = true,
    val profileEdited: Boolean = false,
    val preferencesEdited: Boolean = false,
    val invalidName: Boolean = false,
    val isInitializing: Boolean = true,
    val initializationError: DataFailure? = null,
    val isLoading: Boolean = true,
    val loadError: DataFailure? = null,
    val isSavingProfile: Boolean = false,
    val isSavingPreferences: Boolean = false,
    val profileSaveError: DataFailure? = null,
    val preferencesSaveError: DataFailure? = null,
    val profileSaved: Boolean = false,
    val preferencesSaved: Boolean = false,
    val isSigningOut: Boolean = false,
    val signOutError: DataFailure? = null,
) {
    val canEdit: Boolean get() = userId != null && profile != null && preferences != null &&
        !isInitializing && initializationError == null && !isLoading && loadError == null && !isSigningOut
}

sealed interface ProfileAction {
    data class UpdateName(val value: String) : ProfileAction
    data class SelectAvatar(val id: String) : ProfileAction
    data class ShareActivities(val enabled: Boolean) : ProfileAction
    data object SaveProfile : ProfileAction
    data object SavePreferences : ProfileAction
    data object DiscardProfile : ProfileAction
    data object DiscardPreferences : ProfileAction
    data object Retry : ProfileAction
    data object SignOut : ProfileAction
}
