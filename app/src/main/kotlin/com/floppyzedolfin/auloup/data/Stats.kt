package com.floppyzedolfin.auloup.data

import java.time.Instant
import java.time.ZoneId

/**
 * Pure, Android-free aggregation of blocked-call timestamps into chart data,
 * so it can be unit-tested on the JVM.
 */
object Stats {

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
