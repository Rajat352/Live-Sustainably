package com.livesustainably.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.livesustainably.ui.components.AppTextField
import com.livesustainably.ui.components.ChoicePill
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livesustainably.R
import com.livesustainably.domain.model.DataFailure
import com.livesustainably.domain.model.PRESET_AVATARS
import com.livesustainably.ui.common.dataErrorMessage
import com.livesustainably.ui.components.AppButton
import com.livesustainably.ui.components.AppCard
import com.livesustainably.ui.components.ScreenHeading
import com.livesustainably.ui.components.AvatarBadge
import com.livesustainably.ui.components.HeroCard
import com.livesustainably.ui.components.ErrorContent
import com.livesustainably.ui.components.LoadingContent

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        ScreenHeading(stringResource(R.string.profile_title), stringResource(R.string.profile_subtitle), "YOUR LITTLE CORNER")

        when {
            state.isInitializing -> LoadingContent(stringResource(R.string.profile_initializing))
            state.initializationError != null -> ErrorContent(dataErrorMessage(state.initializationError), { onAction(ProfileAction.Retry) })
            state.isLoading -> LoadingContent(stringResource(R.string.profile_loading))
            state.loadError != null -> ErrorContent(dataErrorMessage(state.loadError), { onAction(ProfileAction.Retry) })
            state.profile != null && state.preferences != null -> {
                HeroCard("Hey, ${state.profile.displayName}!", "A greener you. A happier planet.")
                ProfileEditor(state, onAction)
                PreferencesEditor(state, onAction)
            }
        }

        AppCard {
            Text(stringResource(R.string.profile_account), style = MaterialTheme.typography.titleLarge)
            state.signOutError?.let { StatusText(dataErrorMessage(it), isError = true) }
            AppButton(
                stringResource(if (state.isSigningOut) R.string.profile_signing_out else R.string.profile_sign_out),
                { onAction(ProfileAction.SignOut) }, enabled = state.userId != null && !state.isSigningOut,
            )
        }
    }
}

@Composable
private fun ProfileEditor(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    val enabled = state.canEdit && !state.isSavingProfile

    AppCard {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvatarBadge(state.avatarId)
            Text(stringResource(R.string.profile_identity), style = MaterialTheme.typography.titleLarge)
        }

        AppTextField(
            value = state.displayName,
            onValueChange = { onAction(ProfileAction.UpdateName(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.profile_name)) },
            singleLine = true, enabled = enabled, isError = state.invalidName,
            supportingText = { Text(stringResource(if (state.invalidName) R.string.profile_name_invalid else R.string.profile_name_hint)) },
        )
        Text(stringResource(R.string.profile_avatar), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESET_AVATARS.forEach { id ->
                val (symbol, label) = when (id) {
                    "tree" -> "🌳" to R.string.avatar_tree
                    "sun" -> "☀" to R.string.avatar_sun
                    "earth" -> "🌍" to R.string.avatar_earth
                    else -> "🌿" to R.string.avatar_leaf
                }
                ChoicePill(
                    selected = state.avatarId == id,
                    onClick = { onAction(ProfileAction.SelectAvatar(id)) },
                    label = { Text("$symbol ${stringResource(label)}") }, enabled = enabled,
                )
            }
        }

        state.profileSaveError?.let { SaveError(it) }
        if (state.profileSaved) StatusText(stringResource(R.string.profile_saved))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(
                stringResource(when {
                    state.isSavingProfile -> R.string.profile_saving
                    state.profileSaveError != null -> R.string.profile_retry_save
                    else -> R.string.profile_save
                }),
                { onAction(ProfileAction.SaveProfile) }, enabled = enabled && state.profileEdited,
            )
            TextButton({ onAction(ProfileAction.DiscardProfile) }, enabled = enabled && state.profileEdited) {
                Text(stringResource(R.string.profile_discard))
            }
        }
    }
}

@Composable
private fun PreferencesEditor(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    val enabled = state.canEdit && !state.isSavingPreferences

    AppCard {
        Text(stringResource(R.string.profile_preferences), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.profile_share), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.profile_share_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
        val sharingLabel = stringResource(R.string.profile_share)
        Switch(
            checked = state.shareActivities,
            onCheckedChange = { onAction(ProfileAction.ShareActivities(it)) }, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = sharingLabel },
        )
        state.preferencesSaveError?.let { SaveError(it) }
        if (state.preferencesSaved) StatusText(stringResource(R.string.profile_preferences_saved))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(
                stringResource(when {
                    state.isSavingPreferences -> R.string.profile_saving
                    state.preferencesSaveError != null -> R.string.profile_retry_save
                    else -> R.string.profile_save_preferences
                }),
                { onAction(ProfileAction.SavePreferences) }, enabled = enabled && state.preferencesEdited,
            )
            TextButton({ onAction(ProfileAction.DiscardPreferences) }, enabled = enabled && state.preferencesEdited) {
                Text(stringResource(R.string.profile_discard))
            }
        }
    }
}

@Composable
private fun SaveError(failure: DataFailure) {
    StatusText(stringResource(R.string.profile_save_unconfirmed, dataErrorMessage(failure)), isError = true)
}

@Composable
private fun StatusText(text: String, isError: Boolean = false) {
    Text(text, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
}
