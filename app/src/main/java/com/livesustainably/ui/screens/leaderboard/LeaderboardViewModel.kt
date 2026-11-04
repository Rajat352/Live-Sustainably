@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.livesustainably.ui.screens.leaderboard

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesustainably.domain.model.*
import com.livesustainably.domain.repository.ActivityRepository
import com.livesustainably.domain.repository.AuthRepository
import com.livesustainably.domain.repository.ProfileRepository
import com.livesustainably.ui.common.dataFailure
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

@Stable
data class LeaderboardRow(
    val score: RankedScore,
    val profile: UserProfile? = null,
    val profileError: DataFailure? = null,
)

@Stable
data class LeaderboardUiState(
    val window: ReportWindow,
    val rows: List<LeaderboardRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: DataFailure? = null,
)

@HiltViewModel(assistedFactory = LeaderboardViewModel.Factory::class)
class LeaderboardViewModel @AssistedInject constructor(
    @Assisted selection: ReportSelection,
    auth: AuthRepository,
    private val activities: ActivityRepository,
    private val profiles: ProfileRepository,
    clock: Clock,
) : ViewModel() {
    private val window = selection.window(clock.zone)
    private val retries = MutableStateFlow(0)

    val state = combine(auth.session, retries) { session, _ -> session }.flatMapLatest { session ->
        val loading = LeaderboardUiState(window)
        when (session) {
            UserSession.Loading -> flowOf(loading)
            UserSession.SignedOut -> flowOf(loading.copy(isLoading = false, error = DataFailure.SignedOut))
            is UserSession.SignedIn -> flow {
                emit(loading)
                emitAll(combine(
                    activities.observeActivities(session.userId, window.startMillis, window.endMillis),
                    activities.observePeerScores(window.periodKey),
                ) { own, peers -> rankScores(session.userId, buildReport(own, window), peers) }
                    .flatMapLatest { scores ->
                        combine(scores.map { score ->
                            flow {
                                emit(LeaderboardRow(score))
                                emitAll(profiles.observeProfile(score.userId).map { LeaderboardRow(score, it) })
                            }.catch { error ->
                                if (error is CancellationException) throw error
                                emit(LeaderboardRow(score, profileError = dataFailure(error)))
                            }
                        }) { rows -> loading.copy(rows = rows.toList(), isLoading = false) }
                    })
            }.catch { error ->
                if (error is CancellationException) throw error
                emit(loading.copy(isLoading = false, error = dataFailure(error)))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
        LeaderboardUiState(window))

    fun retry() { retries.update { it + 1 } }

    @AssistedFactory
    interface Factory { fun create(selection: ReportSelection): LeaderboardViewModel }
}
