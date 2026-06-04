package com.floppyzedolfin.auloup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.BlockedCall
import com.floppyzedolfin.auloup.data.Stats
import java.time.ZoneId

/**
 * Two swipeable bar charts of the given blocked [calls]: one per day, one by
 * hour of day. Renders nothing when there are no calls.
 */
@Composable
internal fun StatsSection(calls: List<BlockedCall>, modifier: Modifier = Modifier) {
    if (calls.isEmpty()) return
    val zone = remember { ZoneId.systemDefault() }
    val now = remember(calls) { System.currentTimeMillis() }
    val times = remember(calls) { calls.map { it.timeMillis } }
    val dayStat = remember(times) { Stats.perDay(times, days = 7, zone = zone, nowMillis = now) }
    val hourCounts = remember(times) { Stats.perHour(times, zone) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Total blocked "since the beginning".
        Text(
            pluralStringResource(R.plurals.calls_blocked, calls.size, calls.size),
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalPager(state = pagerState) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (page == 0) R.string.stats_per_day else R.string.stats_by_hour),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (page == 0) {
                    BarChart(dayStat.counts)
                    AxisLabels(dayStat.labels)
                } else {
                    BarChart(hourCounts)
                    // Hour axis 0..24, with 24 pinned to the far right.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        listOf("0", "6", "12", "18", "24").forEach {
                            Text(it, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            repeat(2) { index ->
                val color = if (pagerState.currentPage == index) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
                Box(Modifier.size(8.dp).background(color, CircleShape))
            }
        }
    }
}

/** Evenly-spaced labels under a [BarChart], one per bar. */
@Composable
private fun AxisLabels(labels: List<String>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * A minimal bar chart: one bar per value, with the value printed above each
 * non-zero bar. Bars are bottom-aligned and scaled to the largest value.
 */
@Composable
private fun BarChart(values: List<Int>, modifier: Modifier = Modifier) {
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { value ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (value > 0) value.toString() else "",
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(value.toFloat() / maxValue)
                            .background(barColor, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}
