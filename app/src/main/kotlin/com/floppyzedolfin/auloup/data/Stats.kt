package com.floppyzedolfin.auloup.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Per-day counts with their short weekday labels (oldest first). */
data class DayStat(val counts: List<Int>, val labels: List<String>)

/**
 * Pure, Android-free aggregation of blocked-call timestamps into chart data,
 * so it can be unit-tested on the JVM.
 */
object Stats {

    /**
     * Counts per calendar day for the [days] days ending on the day of
     * [nowMillis], oldest first, with short localized weekday labels.
     */
    fun perDay(timesMillis: List<Long>, days: Int, zone: ZoneId, nowMillis: Long): DayStat {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val start = today.minusDays((days - 1).toLong())
        val byDate = timesMillis
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .groupingBy { it }
            .eachCount()
        val counts = ArrayList<Int>(days)
        val labels = ArrayList<String>(days)
        for (i in 0 until days) {
            val date = start.plusDays(i.toLong())
            counts.add(byDate[date] ?: 0)
            labels.add(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
        }
        return DayStat(counts, labels)
    }

    /** Counts per hour of the day, as 24 values indexed 0..23. */
    fun perHour(timesMillis: List<Long>, zone: ZoneId): List<Int> {
        val counts = IntArray(24)
        for (t in timesMillis) counts[Instant.ofEpochMilli(t).atZone(zone).hour]++
        return counts.toList()
    }

    /**
     * Blocked-call counts keyed by day-of-month (1..N) for the calendar month
     * containing [nowMillis]. Days with no blocked calls are simply absent.
     */
    fun perMonth(timesMillis: List<Long>, zone: ZoneId, nowMillis: Long): Map<Int, Int> {
        val ref = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return perMonth(timesMillis, zone, ref.year, ref.monthValue)
    }

    /** Blocked-call counts keyed by day-of-month for the given [year]/[monthValue]. */
    fun perMonth(timesMillis: List<Long>, zone: ZoneId, year: Int, monthValue: Int): Map<Int, Int> = timesMillis
        .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        .filter { it.year == year && it.monthValue == monthValue }
        .groupingBy { it.dayOfMonth }
        .eachCount()
}
