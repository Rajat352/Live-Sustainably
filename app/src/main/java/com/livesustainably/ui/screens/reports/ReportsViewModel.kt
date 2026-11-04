@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.livesustainably.ui.screens.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesustainably.domain.model.*
import com.livesustainably.domain.repository.ActivityRepository
import com.livesustainably.domain.repository.AuthRepository
import com.livesustainably.ui.common.dataFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

@HiltViewModel
class ReportsViewModel @Inject constructor(
    auth: AuthRepository,
    private val activities: ActivityRepository,
    private val savedState: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {
    private val selection = MutableStateFlow(ReportSelection(
        savedState.get<String>("reportPeriod")?.let(ReportPeriod::valueOf) ?: ReportPeriod.Week,
        savedState.get<Long>("reportAnchor")?.let(LocalDate::ofEpochDay) ?: LocalDate.now(clock),
    ))
    private val category = MutableStateFlow(savedState.get<String>("reportCategory")?.let(ActivityCategory::valueOf))
    private val retries = MutableStateFlow(0)

    private val reads = combine(auth.session, selection, retries) { session, selected, _ -> session to selected }
        .flatMapLatest { (session, selected) ->
            val loading = ReportRead(selected, selected.window(clock.zone))
            when (session) {
                UserSession.Loading -> flowOf(loading)
                UserSession.SignedOut -> flowOf(loading.copy(isLoading = false, error = DataFailure.SignedOut))
                is UserSession.SignedIn -> flow {
                    emit(loading)
                    emitAll(activities.observeActivities(session.userId, loading.window.startMillis, loading.window.endMillis)
                        .map { loading.copy(activities = it, isLoading = false) })
                }.catch { error ->
                    if (error is CancellationException) throw error
                    emit(loading.copy(isLoading = false, error = dataFailure(error)))
                }
            }
        }

    val state = combine(reads, category) { read, selectedCategory ->
        ReportsUiState(
            read.selection, read.window, selectedCategory,
            if (!read.isLoading && read.error == null) buildReport(read.activities, read.window, selectedCategory) else null,
            read.isLoading, read.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
        ReportsUiState(selection.value, selection.value.window(clock.zone), category.value))

    fun onAction(action: ReportsAction) {
        when (action) {
            is ReportsAction.SelectPeriod -> select(selection.value.copy(period = action.period))
            is ReportsAction.ShiftPeriod -> {
                require(action.amount == -1 || action.amount == 1)
                select(selection.value.shifted(action.amount))
            }
            ReportsAction.Today -> select(selection.value.copy(anchorDate = LocalDate.now(clock)))
            is ReportsAction.SelectCategory -> {
                savedState["reportCategory"] = action.category?.name
                category.value = action.category
            }
            ReportsAction.Retry -> retries.update { it + 1 }
        }
    }

    private fun select(value: ReportSelection) {
        savedState["reportPeriod"] = value.period.name
        savedState["reportAnchor"] = value.anchorDate.toEpochDay()
        selection.value = value
    }
}

private data class ReportRead(
    val selection: ReportSelection,
    val window: ReportWindow,
    val activities: List<SustainabilityActivity> = emptyList(),
    val isLoading: Boolean = true,
    val error: DataFailure? = null,
)
