package com.vincentwang.copilot.models

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Pure date math behind the month-calendar UI, kept separate for unit
 * testing. Ported from the MonthGrid enum in CalendarView.swift.
 */
object MonthGrid {
    /** Weekday symbols (single letters) reordered to start at `firstDayOfWeek`. */
    fun orderedWeekdaySymbols(
        firstDayOfWeek: DayOfWeek,
        locale: Locale = Locale.getDefault()
    ): List<String> =
        (0L until 7L).map { offset ->
            firstDayOfWeek.plus(offset).getDisplayName(TextStyle.NARROW, locale)
        }

    /** Every cell of the month grid in order: null for the blank cells
     *  before day 1, then one date per day of the month. */
    fun gridDays(
        month: YearMonth,
        firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY
    ): List<LocalDate?> {
        val first = month.atDay(1)
        val leadingBlanks = ((first.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
        return List(leadingBlanks) { null } +
            (1..month.lengthOfMonth()).map { month.atDay(it) }
    }
}
