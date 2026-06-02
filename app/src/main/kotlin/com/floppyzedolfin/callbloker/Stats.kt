package com.floppyzedolfin.callbloker

import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** One bar of a chart: a value and a (possibly empty) axis label. */
data class Bar(val value: Int, val label: String)

/**
 * Pure, Android-free aggregation of blocked-call timestamps into chart data,
 * so it can be unit-tested on the JVM.
 */
object Stats {

    /**
     * Counts per calendar day for the [days] days ending on the day of
     * [nowMillis], oldest first. Labels are the short localized weekday.
     */
    fun perDay(timesMillis: List<Long>, days: Int, zone: ZoneId, nowMillis: Long): List<Bar> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val start = today.minusDays((days - 1).toLong())
        val byDate = timesMillis
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .groupingBy { it }
            .eachCount()
        return (0 until days).map { i ->
            val date = start.plusDays(i.toLong())
            Bar(byDate[date] ?: 0, date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
        }
    }

    /** Counts per hour of the day (0..23). Labels every six hours. */
    fun perHour(timesMillis: List<Long>, zone: ZoneId): List<Bar> {
        val counts = IntArray(24)
        for (t in timesMillis) counts[Instant.ofEpochMilli(t).atZone(zone).hour]++
        return (0 until 24).map { hour -> Bar(counts[hour], if (hour % 6 == 0) hour.toString() else "") }
    }
}
