package com.floppyzedolfin.auloup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.BlockedCall
import com.floppyzedolfin.auloup.data.CalendarGrid
import com.floppyzedolfin.auloup.data.Stats
import com.floppyzedolfin.auloup.telephony.Countries
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * The shared "blocked calls in a month" view, used by both the full history and
 * the per-prefix screen: a pinned header (total + month navigator + month grid)
 * over a scrolling list of [calls] (which are expected newest-first). Navigating
 * months never goes past the current one, and the list auto-scrolls to the first
 * call in (or before) the selected month. Set [showFlag] to show each call's
 * country flag in its row — used by the mixed-country history; the per-prefix
 * screen shows the flag in its title instead.
 */
@Composable
internal fun MonthlyBlockedCalls(
    calls: List<BlockedCall>,
    showFlag: Boolean,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    val now = remember(calls) { System.currentTimeMillis() }
    val currentMonth = remember(now, zone) {
        YearMonth.from(Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
    }
    // Plain remember (not rememberSaveable): re-entering the screen starts on the
    // current month rather than wherever the user last browsed.
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    val times = remember(calls) { calls.map { it.timeMillis } }
    val countsByDay = remember(times, selectedMonth) {
        Stats.perMonth(times, zone, selectedMonth.year, selectedMonth.monthValue)
    }
    val listState = rememberLazyListState()
    val formatter = rememberCallTimeFormatter()

    // When the month changes, scroll the (newest-first) list to the first call in
    // that month or older.
    LaunchedEffect(selectedMonth) {
        val idx = calls.indexOfFirst {
            val ym = YearMonth.from(Instant.ofEpochMilli(it.timeMillis).atZone(zone).toLocalDate())
            !ym.isAfter(selectedMonth)
        }
        listState.animateScrollToItem(if (idx < 0) (calls.size - 1).coerceAtLeast(0) else idx)
    }

    Column(modifier.fillMaxSize()) {
        Text(
            pluralStringResource(R.plurals.calls_blocked, calls.size, calls.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        MonthNavigator(
            month = selectedMonth,
            canGoNext = selectedMonth.isBefore(currentMonth),
            onPrev = { selectedMonth = selectedMonth.minusMonths(1) },
            onNext = { selectedMonth = selectedMonth.plusMonths(1) },
        )
        MonthCalendar(refDate = selectedMonth.atDay(1), countsByDay = countsByDay)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(calls) { call ->
                BlockedCallRow(
                    call = call,
                    country = Countries.countryForPrefix(call.prefix),
                    formatter = formatter,
                    showFlag = showFlag,
                )
            }
        }
    }
}

/** ‹ Month Year › — the next arrow is disabled at the current month (no future). */
@Composable
private fun MonthNavigator(
    month: YearMonth,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val locale = currentLocale()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = monthYearLabel(month.minusMonths(1), locale),
            )
        }
        Text(
            monthYearLabel(month, locale),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = monthYearLabel(month.plusMonths(1), locale),
                modifier = Modifier.rotate(180f),
            )
        }
    }
}

/**
 * A compact month grid for the month of [refDate]. Weekday columns start on the
 * locale's first day of the week. Each day with blocked calls shows the count in
 * a filled circle; quiet days show an empty outline.
 */
@Composable
private fun MonthCalendar(refDate: LocalDate, countsByDay: Map<Int, Int>) {
    val locale = currentLocale()
    val yearMonth = remember(refDate) { YearMonth.from(refDate) }
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val weekdays = remember(firstDayOfWeek) { CalendarGrid.weekdayOrder(firstDayOfWeek) }
    val weeks = remember(yearMonth, firstDayOfWeek) { CalendarGrid.weeks(yearMonth, firstDayOfWeek) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) DayCircle(count = countsByDay[day] ?: 0)
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
