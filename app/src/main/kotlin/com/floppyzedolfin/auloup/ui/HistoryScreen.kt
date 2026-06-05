package com.floppyzedolfin.auloup.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.data.Stats
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.PhoneFormat
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/** The full history of blocked calls across every prefix, most recent first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(repository: PrefixRepository, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    val calls by repository.allCalls.collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }
    val formatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = { LogoNavIcon(onBack = onBack) },
                actions = {
                    if (calls.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.clear_history),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (calls.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                Text(
                    stringResource(R.string.no_calls_yet),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            val zone = remember { ZoneId.systemDefault() }
            val now = remember(calls) { System.currentTimeMillis() }
            // Current month resets every time History is (re-)entered — selectedMonth
            // is plain remember, so leaving and returning starts on the current month.
            val currentMonth = remember(now, zone) {
                YearMonth.from(Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
            }
            var selectedMonth by remember { mutableStateOf(currentMonth) }
            val times = remember(calls) { calls.map { it.timeMillis } }
            val countsByDay = remember(times, selectedMonth) {
                Stats.perMonth(times, zone, selectedMonth.year, selectedMonth.monthValue)
            }
            val listState = rememberLazyListState()

            // When the month changes, scroll the list (newest-first) to the first
            // call in that month or older.
            LaunchedEffect(selectedMonth) {
                val idx = calls.indexOfFirst {
                    val ym = YearMonth.from(Instant.ofEpochMilli(it.timeMillis).atZone(zone).toLocalDate())
                    !ym.isAfter(selectedMonth)
                }
                listState.animateScrollToItem(if (idx < 0) (calls.size - 1).coerceAtLeast(0) else idx)
            }

            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // Pinned header: total, month navigator, and the month grid.
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
                MonthCalendar(
                    refDate = selectedMonth.atDay(1),
                    countsByDay = countsByDay,
                    showMonthLabel = false,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(calls) { call ->
                        val country = Countries.countryForPrefix(call.prefix)
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = country?.flag?.let { flag -> { Text(flag) } },
                            headlineContent = {
                                Text(
                                    if (call.number.isBlank()) {
                                        stringResource(R.string.unknown_number)
                                    } else {
                                        PhoneFormat.number(call.number, country)
                                    },
                                )
                            },
                            supportingContent = { Text(formatter.format(Date(call.timeMillis))) },
                        )
                    }
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(stringResource(R.string.clear_history)) },
                text = { Text(stringResource(R.string.clear_history_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch { repository.clearHistory() }
                        showClearDialog = false
                    }) {
                        Text(
                            stringResource(R.string.remove),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

/** Localized "Month Year", e.g. "June 2026" / "Juin 2026". */
private fun monthLabel(month: YearMonth, locale: Locale): String =
    month.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) } +
        " " + month.year

/** ‹ Month Year › — the next arrow is disabled at the current month (no future). */
@Composable
private fun MonthNavigator(
    month: YearMonth,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = monthLabel(month.minusMonths(1), locale),
            )
        }
        Text(
            monthLabel(month, locale),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = monthLabel(month.plusMonths(1), locale),
                modifier = Modifier.rotate(180f),
            )
        }
    }
}
