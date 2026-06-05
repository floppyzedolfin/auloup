package com.floppyzedolfin.auloup

import com.floppyzedolfin.auloup.data.Stats
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class StatsTest {

    private val utc = ZoneOffset.UTC

    private fun millis(y: Int, mo: Int, d: Int, h: Int): Long =
        LocalDateTime.of(y, mo, d, h, 0).toInstant(utc).toEpochMilli()

    @Test
    fun perMonth_countsByDayOfCurrentMonthOnly() {
        val now = millis(2026, 6, 15, 12)
        val times = listOf(
            millis(2026, 6, 5, 9),
            millis(2026, 6, 5, 20),
            millis(2026, 6, 20, 10),
            millis(2026, 5, 31, 23), // previous month -> excluded
            millis(2026, 7, 1, 0), // next month -> excluded
        )
        val byDay = Stats.perMonth(times, utc, now)
        assertEquals(mapOf(5 to 2, 20 to 1), byDay)
    }
}
