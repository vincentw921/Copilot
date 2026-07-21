package com.vincentwang.copilot.features.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vincentwang.copilot.features.logbook.FlightDetailSheet
import com.vincentwang.copilot.features.logbook.LogbookViewModel
import com.vincentwang.copilot.models.FlightEntry
import com.vincentwang.copilot.ui.components.FlightRow
import com.vincentwang.copilot.ui.components.FormRow
import com.vincentwang.copilot.ui.components.FormSection
import com.vincentwang.copilot.ui.components.MonthCalendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val sectionDateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

/**
 * Browse the logbook by date. Days with logged flights are highlighted
 * green; tap a day to list (and edit) that day's flights. Ported from
 * CalendarView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(model: LogbookViewModel = viewModel()) {
    val items by model.items.collectAsState()
    val savedAircraft by model.aircraft.collectAsState()

    // The day whose flights are listed below the calendar.
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var viewingItemId by remember { mutableStateOf<String?>(null) }

    val entriesById = items.associate { it.id to FlightEntry.fromItem(it) }
    // Every date that has at least one flight.
    val flightDays = entriesById.values.map { it.date }.toSet()
    // Flights logged on the selected calendar day.
    val flightsOnSelectedDay = items.filter { entriesById[it.id]?.date == selectedDate }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calendar") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            FormSection {
                MonthCalendar(
                    selectedDate = selectedDate,
                    highlightedDays = flightDays,
                    onDaySelected = { selectedDate = it }
                )
            }

            FormSection(title = selectedDate.format(sectionDateFormat)) {
                if (flightsOnSelectedDay.isEmpty()) {
                    FormRow {
                        Text(
                            "No flights on this day.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    flightsOnSelectedDay.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider()
                        Column(modifier = Modifier.clickable { viewingItemId = item.id }) {
                            FlightRow(entriesById.getValue(item.id))
                        }
                    }
                }
            }
        }
    }

    val viewingItem = items.firstOrNull { it.id == viewingItemId }
    if (viewingItem != null) {
        FlightDetailSheet(
            entry = FlightEntry.fromItem(viewingItem),
            savedAircraft = savedAircraft,
            onSave = model::saveEntry,
            onDismiss = { viewingItemId = null }
        )
    }
}
