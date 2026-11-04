package com.livesustainably.ui.screens.reports

import androidx.compose.runtime.Stable
import com.livesustainably.domain.model.ActivityCategory
import com.livesustainably.domain.model.DataFailure
import com.livesustainably.domain.model.ReportPeriod
import com.livesustainably.domain.model.ReportSelection
import com.livesustainably.domain.model.ReportWindow
import com.livesustainably.domain.model.SustainabilityReport

@Stable
data class ReportsUiState(
    val selection: ReportSelection,
    val window: ReportWindow,
    val category: ActivityCategory? = null,
    val report: SustainabilityReport? = null,
    val isLoading: Boolean = true,
    val error: DataFailure? = null,
)

sealed interface ReportsAction {
    data class SelectPeriod(val period: ReportPeriod) : ReportsAction
    data class SelectCategory(val category: ActivityCategory?) : ReportsAction
    data class ShiftPeriod(val amount: Int) : ReportsAction
    data object Today : ReportsAction
    data object Retry : ReportsAction
}
