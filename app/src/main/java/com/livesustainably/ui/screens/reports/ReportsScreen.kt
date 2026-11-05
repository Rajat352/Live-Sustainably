package com.livesustainably.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.livesustainably.R
import com.livesustainably.domain.model.*
import com.livesustainably.ui.common.dataErrorMessage
import com.livesustainably.ui.common.formatSaving
import com.livesustainably.ui.components.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onAction: (ReportsAction) -> Unit,
    onLeaderboard: (ReportSelection) -> Unit
) {
    var showHowItWorks by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {

        item { ScreenHeading(stringResource(R.string.reports_title), stringResource(R.string.reports_subtitle), "LOOK AT YOU GROW") }

        state.report?.let { report ->
            item {
                HeroCard(formatSaving(report.total.savedKgCo2e) + " kg\nCO₂e saved", "Little choices. Growing impact.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricTile("⭐", report.total.points.toString(), "Points", Modifier.weight(1f), onHero = true)
                        MetricTile("🌱", report.total.activityCount.toString(), "Activities", Modifier.weight(1f), onHero = true)
                    }
                }
            }
        }

        item {
            AppCard {
                Text("Find your rhythm", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportPeriod.entries.forEach { period ->
                        ChoicePill(
                            selected = state.selection.period == period,
                            onClick = { onAction(ReportsAction.SelectPeriod(period)) },
                            label = { Text(stringResource(if (period == ReportPeriod.Week) R.string.reports_weekly else R.string.reports_monthly)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(formatReportWindow(state.window), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onAction(ReportsAction.ShiftPeriod(-1)) }) { Text("‹ " + stringResource(R.string.reports_previous)) }
                    TextButton(onClick = { onAction(ReportsAction.Today) }) { Text(stringResource(R.string.reports_current)) }
                    TextButton(onClick = { onAction(ReportsAction.ShiftPeriod(1)) }) { Text(stringResource(R.string.reports_next) + " ›") }
                }
                Text(stringResource(R.string.reports_category), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill(state.category == null, { onAction(ReportsAction.SelectCategory(null)) }, label = { Text(stringResource(R.string.reports_all)) })
                    ActivityCategory.entries.forEach { category ->
                        ChoicePill(state.category == category, { onAction(ReportsAction.SelectCategory(category)) },
                            label = { Text(categorySymbol(category) + " " + categoryLabel(category)) })
                    }
                }
            }
        }

        item { AppButton(stringResource(R.string.reports_leaderboard), { onLeaderboard(state.selection) }, Modifier.fillMaxWidth()) }

        when {
            state.isLoading -> item { LoadingContent(stringResource(R.string.reports_loading)) }
            state.error != null -> item { ErrorContent(dataErrorMessage(state.error), { onAction(ReportsAction.Retry) }) }
            else -> state.report?.let { report ->
                if (report.total.activityCount == 0) item {
                    EmptyContent(stringResource(R.string.reports_empty_title), stringResource(R.string.reports_empty_message))
                }
                item { DailyChart(report) }
                item {
                    AppCard {
                        Text(stringResource(R.string.reports_breakdown), style = MaterialTheme.typography.titleLarge)
                        report.categories.forEach { (category, total) ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(categorySymbol(category), style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.clearAndSetSemantics {})
                                Column(Modifier.weight(1f)) {
                                    Text(categoryLabel(category), style = MaterialTheme.typography.titleMedium)
                                    Text(formatSaving(total.savedKgCo2e) + " kg CO₂e · " + total.points + " points",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            LinearProgressIndicator(
                                progress = { if (report.total.savedKgCo2e == 0.0) 0f else (total.savedKgCo2e / report.total.savedKgCo2e).toFloat() },
                                modifier = Modifier.fillMaxWidth().height(9.dp),
                                color = when (category) {
                                    ActivityCategory.Transport -> com.livesustainably.ui.theme.HeroCyan
                                    ActivityCategory.Food -> com.livesustainably.ui.theme.LeafGreen
                                    ActivityCategory.Shopping -> com.livesustainably.ui.theme.Coral
                                }, trackColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            )
                        }
                    }
                }
                item {
                    TextButton(onClick = { showHowItWorks = !showHowItWorks }) {
                        Text(stringResource(R.string.reports_assumptions_title) + if (showHowItWorks) "  −" else "  +")
                    }
                    if (showHowItWorks) {
                        Text(stringResource(R.string.reports_assumptions), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.reports_timezone, state.window.zone.id), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

internal fun categorySymbol(category: ActivityCategory): String = when (category) {
    ActivityCategory.Transport -> "🚲"
    ActivityCategory.Food -> "🥑"
    ActivityCategory.Shopping -> "🛍️"
}

@Composable
private fun DailyChart(
    report: SustainabilityReport
) {
    var showValues by rememberSaveable { mutableStateOf(false) }
    val model = remember(report.days) {
        CartesianChartModel(ColumnCartesianLayerModel.build { series(report.days.map { it.total.savedKgCo2e }) })
    }
    val dateFormat = DateTimeFormatter.ofPattern("d MMM")
    val xFormatter = remember(report.window) {
        CartesianValueFormatter { _, value, _ -> report.days.getOrNull(value.toInt())?.date?.format(dateFormat).orEmpty() }
    }
    val summary = pluralStringResource(R.plurals.reports_chart_summary, report.days.size, report.days.size, formatSaving(report.total.savedKgCo2e))
    AppCard {
        Text(stringResource(R.string.reports_daily_chart), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.reports_chart_units), style = MaterialTheme.typography.bodySmall)
        ProvideVicoTheme(rememberM3VicoTheme()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter),
                ),
                model = model,
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                modifier = Modifier.fillMaxWidth().height(240.dp).clearAndSetSemantics { contentDescription = summary },
            )
        }
        TextButton(onClick = { showValues = !showValues }) {
            Text(stringResource(if (showValues) R.string.reports_hide_values else R.string.reports_show_values))
        }
        if (showValues) report.days.forEach { day ->
            Text(stringResource(R.string.reports_daily_value, day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), formatSaving(day.total.savedKgCo2e), day.total.points))
        }
    }
}

internal fun formatReportWindow(window: ReportWindow): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    return "${window.startDate.format(formatter)} – ${window.endDateExclusive.minusDays(1).format(formatter)}"
}

@Composable
internal fun categoryLabel(category: ActivityCategory): String = stringResource(when (category) {
    ActivityCategory.Transport -> R.string.category_transport
    ActivityCategory.Food -> R.string.category_food
    ActivityCategory.Shopping -> R.string.category_shopping
})
