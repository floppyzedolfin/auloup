package com.floppyzedolfin.auloup

import com.floppyzedolfin.auloup.data.CalendarGrid
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.YearMonth

class CalendarGridTest {

    @Test
    fun weeks_padsLeadingBlanksToAlignTheFirst() {
        // June 2026: the 1st is a Monday.
        val weeks = CalendarGrid.weeks(YearMonth.of(2026, 6), DayOfWeek.SUNDAY)
        // Sunday-first: row starts Sun, so Monday the 1st sits in column 1 (one blank).
        assertEquals(listOf(null, 1, 2, 3, 4, 5, 6), weeks.first())
        // Monday-first: the 1st sits in column 0 (no leading blank).
        val mondayFirst = CalendarGrid.weeks(YearMonth.of(2026, 6), DayOfWeek.MONDAY)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), mondayFirst.first())
    }

    @Test
    fun weeks_coverEveryDayInSevenColumnRows() {
        val weeks = CalendarGrid.weeks(YearMonth.of(2026, 6), DayOfWeek.MONDAY)
        weeks.forEach { assertEquals(7, it.size) }
        assertEquals((1..30).toList(), weeks.flatten().filterNotNull())
    }

    @Test
    fun weekdayOrder_startsOnTheGivenDay() {
        assertEquals(
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
            ),
            CalendarGrid.weekdayOrder(DayOfWeek.MONDAY),
        )
        assertEquals(DayOfWeek.SUNDAY, CalendarGrid.weekdayOrder(DayOfWeek.SUNDAY).first())
    }
}
