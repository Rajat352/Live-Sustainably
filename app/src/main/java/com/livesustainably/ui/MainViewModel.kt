package com.livesustainably.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesustainably.domain.model.UserSession
import com.livesustainably.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SessionUiState(
    val session: UserSession,
    val revision: Int = 0
) {
    // For unique sessions
    val navigationKey: String get() = when (session) {
        is UserSession.SignedIn -> "user:${session.userId}:$revision"
        else -> "signed-out:$revision"
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    repository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Keep observing while backgrounded so account transitions invalidate old navigation state
    val session = repository.session.map { session ->
        val identity = (session as? UserSession.SignedIn)?.userId ?: "signed-out"
        val previousIdentity = savedStateHandle.get<String>("sessionIdentity")
        val revision = (savedStateHandle.get<Int>("sessionRevision") ?: 0) +
            if (previousIdentity != null && previousIdentity != identity) 1 else 0
        savedStateHandle["sessionIdentity"] = identity
        savedStateHandle["sessionRevision"] = revision
        SessionUiState(session, revision)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionUiState(UserSession.Loading))
}
