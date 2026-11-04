@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.livesustainably.ui.screens.friendsmap

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesustainably.domain.model.DataFailure
import com.livesustainably.domain.model.DemoFriend
import com.livesustainably.domain.model.DemoMapConfiguration
import com.livesustainably.domain.model.UserSession
import com.livesustainably.domain.model.nearbyDemoFriends
import com.livesustainably.domain.repository.AuthRepository
import com.livesustainably.domain.repository.FriendsRepository
import com.livesustainably.ui.common.dataFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

@Stable
data class FriendsMapUiState(
    val configuration: DemoMapConfiguration? = null,
    val friends: List<DemoFriend> = emptyList(),
    val selectedFriendId: String? = null,
    val isLoading: Boolean = true,
    val error: DataFailure? = null,
) {
    val selectedFriend: DemoFriend? get() = friends.find { it.profile.userId == selectedFriendId }
}

@HiltViewModel
class FriendsMapViewModel @Inject constructor(
    auth: AuthRepository,
    private val friends: FriendsRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val retries = MutableStateFlow(0)
    private val selection = savedState.getStateFlow<String?>("selectedDemoFriend", null)

    private val reads = combine(auth.session, retries) { session, _ -> session }
        .flatMapLatest { session ->
            when (session) {
                UserSession.Loading -> flowOf(FriendsMapUiState())
                UserSession.SignedOut -> flowOf(FriendsMapUiState(isLoading = false, error = DataFailure.SignedOut))
                is UserSession.SignedIn -> flow {
                    emit(FriendsMapUiState())
                    emitAll(combine(
                        friends.observeMapConfiguration(),
                        friends.observeDemoFriends(session.userId),
                    ) { configuration, people ->
                        FriendsMapUiState(configuration, nearbyDemoFriends(people, configuration), isLoading = false)
                    })
                }.catch { error ->
                    if (error is CancellationException) throw error
                    emit(FriendsMapUiState(isLoading = false, error = dataFailure(error)))
                }
            }
        }

    val state = combine(reads, selection) { read, selectedId ->
        read.copy(selectedFriendId = selectedId?.takeIf { id -> read.friends.any { it.profile.userId == id } })
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
        FriendsMapUiState(),
    )

    fun selectFriend(userId: String?) {
        if (userId == null || state.value.friends.any { it.profile.userId == userId }) {
            savedState["selectedDemoFriend"] = userId
        }
    }

    fun retry() { retries.update { it + 1 } }
}
