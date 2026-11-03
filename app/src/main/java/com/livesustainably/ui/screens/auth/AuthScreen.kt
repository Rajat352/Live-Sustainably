package com.livesustainably.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livesustainably.ui.components.AppButton
import com.livesustainably.ui.components.AppCard

@Composable
fun AuthScreen(
    isSignup: Boolean,
    onSwitchForm: () -> Unit
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var passwordVisible by remember { mutableStateOf(false) }
    val submit = {
        keyboard?.hide()
        focus.clearFocus()
        viewModel.submit(isSignup)
    }

    BackHandler(enabled = state.isSubmitting) {}

    Box(
        Modifier.fillMaxSize().imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("Live Sustainably", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isSignup) "Small steps. Lasting change." else "Welcome back", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (isSignup) "Create an account to start your sustainability journey." else "Sign in to continue your sustainability journey.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppCard {
                Text(if (isSignup) "Create your account" else "Sign in with email", style = MaterialTheme.typography.titleLarge)
                if (isSignup) {
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = viewModel::updateDisplayName,
                        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.PersonFullName },
                        label = { Text("Display name") },
                        singleLine = true,
                        enabled = !state.isSubmitting,
                        isError = state.displayNameError != null,
                        supportingText = state.displayNameError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    )
                }
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::updateEmail,
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress },
                    label = { Text("Email address") },
                    singleLine = true,
                    enabled = !state.isSubmitting,
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    modifier = Modifier.fillMaxWidth().semantics { contentType = if (isSignup) ContentType.NewPassword else ContentType.Password },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !state.isSubmitting,
                    isError = state.passwordError != null,
                    supportingText = { Text(state.passwordError ?: if (isSignup) "Use at least 6 characters." else "Your password is case sensitive.") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "Hide" else "Show")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (isSignup) ImeAction.Next else ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onNext = { focus.moveFocus(FocusDirection.Down) },
                        onDone = { submit() },
                    ),
                )
                if (isSignup) {
                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.NewPassword },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        enabled = !state.isSubmitting,
                        isError = state.confirmPasswordError != null,
                        supportingText = state.confirmPasswordError?.let { { Text(it) } },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                }
                state.error?.let { AuthErrorText(it) }
                if (state.isSubmitting) {
                    Text(if (isSignup) "Creating your account…" else "Signing in…", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                AppButton(
                    text = if (isSignup) "Create account" else "Sign in",
                    onClick = submit,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TextButton(onClick = onSwitchForm, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                Text(if (isSignup) "Already have an account? Sign in" else "New here? Create an account")
            }
        }
    }
}

@Composable
internal fun AuthErrorText(message: String) {
    Text(
        message,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}
