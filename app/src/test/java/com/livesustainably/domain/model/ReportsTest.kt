package com.livesustainably.domain.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ReportsTest {
    @Test
    fun `weekly category totals respect local DST boundaries and count each activity once`() {
        val zone = ZoneId.of("America/New_York")
        val window = ReportSelection(ReportPeriod.Week, LocalDate.of(2026, 3, 8)).window(zone)
        val sundayNight = LocalDate.of(2026, 3, 8).atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        fun activity(id: String, at: Long, category: ActivityCategory, kg: Double, points: Int) =
            SustainabilityActivity(id, at, category, id, kg, points, isDemo = false)
        val first = activity("monday", window.startMillis, ActivityCategory.Transport, 1.25, Int.MAX_VALUE)
        val report = buildReport(listOf(
            activity("before", window.startMillis - 1, ActivityCategory.Transport, 100.0, 100),
            first,
            first,
            activity("sunday", sundayNight, ActivityCategory.Transport, 2.75, Int.MAX_VALUE),
            activity("food", sundayNight, ActivityCategory.Food, 100.0, 100),
            activity("next-week", window.endMillis, ActivityCategory.Transport, 100.0, 100),
        ), window, ActivityCategory.Transport)

        assertEquals(LocalDate.of(2026, 3, 2), window.startDate)
        assertEquals(LocalDate.of(2026, 3, 9), window.endDateExclusive)
        assertEquals(167L * 60 * 60 * 1000, window.endMillis - window.startMillis)
        assertEquals(ReportTotal(4.0, 2L * Int.MAX_VALUE, 2), report.total)
        assertEquals(7, report.days.size)
        assertEquals(1, report.days.first().total.activityCount)
        assertEquals(1, report.days.last().total.activityCount)
        assertTrue(report.days.subList(1, 6).all { it.total == ReportTotal(0.0, 0, 0) })
        assertEquals(report.total, report.categories.getValue(ActivityCategory.Transport))
        assertEquals(ReportTotal(0.0, 0, 0), report.categories.getValue(ActivityCategory.Food))
        assertEquals(report.total.points, report.days.sumOf { it.total.points })
        assertEquals(report.total.savedKgCo2e, report.days.sumOf { it.total.savedKgCo2e }, 0.0)
    }
}
