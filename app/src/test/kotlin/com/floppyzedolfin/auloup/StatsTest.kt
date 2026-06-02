package com.floppyzedolfin.auloup

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class StatsTest {

    private val utc = ZoneOffset.UTC

    private fun millis(y: Int, mo: Int, d: Int, h: Int): Long =
        LocalDateTime.of(y, mo, d, h, 0).toInstant(utc).toEpochMilli()

    @Test
    fun perHour_has24BucketsAndCountsByLocalHour() {
        val times = listOf(
            millis(2026, 6, 2, 9),
            millis(2026, 6, 2, 9),
            millis(2026, 6, 1, 14),
        )
        val counts = Stats.perHour(times, utc)
        assertEquals(24, counts.size)
        assertEquals(2, counts[9])
        assertEquals(1, counts[14])
        assertEquals(0, counts[0])
    }

    @Test
    fun perDay_countsByCalendarDayEndingToday() {
        val now = millis(2026, 6, 2, 12)
        // Two calls today (08:00, 20:00) and one yesterday (10:00).
        val times = listOf(
            millis(2026, 6, 2, 8),
            millis(2026, 6, 2, 20),
            millis(2026, 6, 1, 10),
        )
        val stat = Stats.perDay(times, days = 7, zone = utc, nowMillis = now)
        assertEquals(7, stat.counts.size)
        assertEquals(7, stat.labels.size)
        assertEquals(2, stat.counts.last()) // today is the last bucket
        assertEquals(1, stat.counts[stat.counts.size - 2]) // yesterday
        assertEquals(0, stat.counts.first()) // 6 days ago
    }

    @Test
    fun perDay_ignoresCallsOutsideTheWindow() {
        val now = millis(2026, 6, 2, 12)
        val old = millis(2026, 5, 1, 12) // well outside the 7-day window
        val stat = Stats.perDay(listOf(old), days = 7, zone = utc, nowMillis = now)
        assertEquals(0, stat.counts.sum())
    }
}
