package com.livesustainably.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livesustainably.ui.components.AppButton
import com.livesustainably.ui.components.AppCard
import com.livesustainably.ui.screens.auth.AuthErrorText

@Composable
fun ProfileScreen(
    state: UiState,
    onSignOut: () -> Unit,
    onRetryProfile: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Your profile",
            style = MaterialTheme.typography.headlineLarge
        )

        AppCard {
            Text(
                state.profile?.displayName ?: if (state.isLoadingProfile) "Loading your profile…" else "Your account",
                style = MaterialTheme.typography.titleLarge,
            )
            Text("You’re signed in to Live Sustainably.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.error?.let {
                AuthErrorText(it)
                AppButton("Try again", onRetryProfile, enabled = !state.isLoadingProfile && !state.isSigningOut)
            }
            AppButton(if (state.isSigningOut) "Signing out…" else "Sign out", onSignOut, enabled = !state.isSigningOut)
        }
    }
}