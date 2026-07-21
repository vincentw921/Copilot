package com.vincentwang.copilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vincentwang.copilot.models.MonthGrid
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val flightGreen = Color(0xFF34A853)

/**
 * A single-month calendar grid, ported from MonthCalendarView in
 * CalendarView.swift. Days in `highlightedDays` get a green circle; the
 * selected day gets a ring; today's number is tinted. Chevrons move
 * between months, and tapping the title swaps the grid for month/year
 * pickers. Future days are blocked (61.51 records what already happened).
 */
@Composable
fun MonthCalendar(
    selectedDate: LocalDate,
    highlightedDays: Set<LocalDate>,
    onDaySelected: (LocalDate) -> Unit
) {
    // Open on the month of the current selection (today for the Calendar
    // tab, the flight's date when editing an old entry).
    var displayedMonth by rememberSaveable { mutableStateOf(YearMonth.from(selectedDate)) }
    var showMonthYearPicker by rememberSaveable { mutableStateOf(false) }

    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        // Header: month title toggles the pickers; chevrons page months.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showMonthYearPicker = !showMonthYearPicker }) {
                Text(
                    displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                        " " + displayedMonth.year,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (showMonthYearPicker) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (showMonthYearPicker) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.rotate(if (showMonthYearPicker) 90f else 0f)
                )
            }
            Box(modifier = Modifier.weight(1f))
            // The paging chevrons hide while the pickers are up, matching
            // the system date picker.
            if (!showMonthYearPicker) {
                IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                }
                IconButton(
                    onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                    enabled = displayedMonth < currentMonth
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                }
            }
        }

        if (showMonthYearPicker) {
            MonthYearPicker(
                displayedMonth = displayedMonth,
                onMonthChange = { target ->
                    // Clamp to the current month so the pickers can't
                    // wander into the blocked-out future.
                    displayedMonth = if (target > currentMonth) currentMonth else target
                }
            )
        } else {
            WeekdayLabels()
            DayGrid(
                displayedMonth = displayedMonth,
                selectedDate = selectedDate,
                highlightedDays = highlightedDays,
                today = today,
                onDaySelected = onDaySelected
            )
        }
    }
}

/** Localized single-letter weekday header row. */
@Composable
private fun WeekdayLabels() {
    val symbols = remember { MonthGrid.orderedWeekdaySymbols(DayOfWeek.SUNDAY) }
    Row(modifier = Modifier.fillMaxWidth()) {
        symbols.forEach { symbol ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    symbol,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** The 7-column day grid, with null cells padding the first week. */
@Composable
private fun DayGrid(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    highlightedDays: Set<LocalDate>,
    today: LocalDate,
    onDaySelected: (LocalDate) -> Unit
) {
    val cells = MonthGrid.gridDays(displayedMonth, DayOfWeek.SUNDAY)
    Column(modifier = Modifier.fillMaxWidth()) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            DayCell(day, selectedDate, highlightedDays, today, onDaySelected)
                        }
                    }
                }
                repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** One tappable day: green fill when a flight exists, ring when selected,
 *  tinted number for today. Future days are greyed out and can't be tapped. */
@Composable
private fun DayCell(
    day: LocalDate,
    selectedDate: LocalDate,
    highlightedDays: Set<LocalDate>,
    today: LocalDate,
    onDaySelected: (LocalDate) -> Unit
) {
    val hasFlight = day in highlightedDays
    val isSelected = day == selectedDate
    val isFuture = day > today
    val isToday = day == today

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = if (hasFlight) flightGreen else Color.Transparent,
                shape = CircleShape
            )
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !isFuture) { onDaySelected(day) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${day.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (hasFlight || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isFuture -> MaterialTheme.colorScheme.outlineVariant
                hasFlight -> Color.White
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/** Side-by-side month and year lists shown in place of the day grid,
 *  standing in for the iOS wheel pickers. */
@Composable
private fun MonthYearPicker(
    displayedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    val months = remember {
        (1..12).map { java.time.Month.of(it).getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    }
    // The current year back through 60 years of logbook history,
    // ascending so scrolling down moves toward the present.
    val years = remember { ((LocalDate.now().year - 60)..LocalDate.now().year).toList() }

    Row(modifier = Modifier.fillMaxWidth().height(214.dp)) {
        val monthState = rememberLazyListState()
        val yearState = rememberLazyListState()
        LaunchedEffect(Unit) {
            monthState.scrollToItem((displayedMonth.monthValue - 1).coerceAtLeast(0))
            yearState.scrollToItem(years.indexOf(displayedMonth.year).coerceAtLeast(0))
        }

        LazyColumn(state = monthState, modifier = Modifier.weight(1f)) {
            items(months.indices.toList()) { index ->
                val selected = displayedMonth.monthValue == index + 1
                Text(
                    months[index],
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMonthChange(YearMonth.of(displayedMonth.year, index + 1))
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }
        LazyColumn(state = yearState, modifier = Modifier.weight(1f)) {
            items(years) { year ->
                val selected = displayedMonth.year == year
                Text(
                    "$year",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMonthChange(YearMonth.of(year, displayedMonth.monthValue))
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }
    }
}
