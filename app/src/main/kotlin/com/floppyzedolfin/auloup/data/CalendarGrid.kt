package com.floppyzedolfin.auloup.data

import java.time.DayOfWeek
import java.time.YearMonth

/**
 * Pure, Android-free calendar layout: arranges a month into weeks of 7 cells,
 * so the UI only has to render the grid (no index math). Each cell is the
 * day-of-month (1..N) or null for the leading/trailing blanks that align the
 * 1st under [firstDayOfWeek].
 */
object CalendarGrid {

    fun weeks(yearMonth: YearMonth, firstDayOfWeek: DayOfWeek): List<List<Int?>> {
        val lead = (yearMonth.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val days: List<Int?> = (1..yearMonth.lengthOfMonth()).toList()
        val padded = List<Int?>(lead) { null } + days
        val trailing = (7 - padded.size % 7) % 7
        return (padded + List<Int?>(trailing) { null }).chunked(7)
    }

    /** The 7 weekday columns, in display order starting from [firstDayOfWeek]. */
    fun weekdayOrder(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
        (0L until 7L).map { firstDayOfWeek.plus(it) }
}
