package com.floppyzedolfin.auloup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.BlockedCall
import com.floppyzedolfin.auloup.data.Stats
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * Blocked-call stats for the given [calls]: an optional total, then a compact
 * calendar of the current month with a count circle on each day that blocked
 * a call. Renders nothing when there are no calls.
 */
@Composable
internal fun StatsSection(
    calls: List<BlockedCall>,
    modifier: Modifier = Modifier,
    showTotal: Boolean = true,
) {
    if (calls.isEmpty()) return
    val zone = remember { ZoneId.systemDefault() }
    val now = remember(calls) { System.currentTimeMillis() }
    val times = remember(calls) { calls.map { it.timeMillis } }
    val countsByDay = remember(times) { Stats.perMonth(times, zone, now) }
    val refDate = remember(now) { java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate() }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showTotal) {
            Text(
                pluralStringResource(R.plurals.calls_blocked, calls.size, calls.size),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        MonthCalendar(refDate = refDate, countsByDay = countsByDay)
    }
}

/**
 * A compact month grid for the month of [refDate]. Weekday columns start on the
 * locale's first day of the week. Each day with blocked calls shows the count in
 * a filled circle; quiet days show an empty outline.
 */
@Composable
internal fun MonthCalendar(
    refDate: LocalDate,
    countsByDay: Map<Int, Int>,
    showMonthLabel: Boolean = true,
) {
    val locale = currentLocale()
    val yearMonth = remember(refDate) { YearMonth.from(refDate) }
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val weekdays = remember(firstDayOfWeek) { (0L until 7L).map { firstDayOfWeek.plus(it) } }
    val daysInMonth = yearMonth.lengthOfMonth()
    // Empty cells before the 1st so it lands in its weekday column.
    val lead = (yearMonth.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val rows = (lead + daysInMonth + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (showMonthLabel) {
            Text(
                monthYearLabel(yearMonth, locale),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { dow ->
                Text(
                    dow.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (week in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (dowIndex in 0 until 7) {
                    val day = week * 7 + dowIndex - lead + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day in 1..daysInMonth) {
                            DayCircle(count = countsByDay[day] ?: 0)
                        }
                    }
                }
            }
        }
    }
}

/** One day: a filled circle with the count if any calls were blocked, else an outline. */
@Composable
private fun DayCircle(count: Int) {
    val base = Modifier.size(28.dp)
    if (count > 0) {
        Box(
            modifier = base.background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    } else {
        Box(modifier = base.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
    }
}
