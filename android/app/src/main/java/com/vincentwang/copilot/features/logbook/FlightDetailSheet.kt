package com.vincentwang.copilot.features.logbook

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vincentwang.copilot.data.AircraftProfile
import com.vincentwang.copilot.models.AirportDatabase
import com.vincentwang.copilot.models.FlightEntry
import com.vincentwang.copilot.ui.components.CountRow
import com.vincentwang.copilot.ui.components.FormSection
import com.vincentwang.copilot.ui.components.FullScreenSheet
import com.vincentwang.copilot.ui.components.HoursRow
import com.vincentwang.copilot.ui.components.LabeledRow
import java.time.format.DateTimeFormatter
import java.util.Locale

private val titleDateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

/**
 * Read-only preview of one logbook entry, shown when a flight is tapped
 * in the Logbook or Calendar. Ported from FlightDetailView.swift.
 * Nothing here is editable — the Edit button opens the form, and Done
 * closes the preview.
 */
@Composable
fun FlightDetailSheet(
    entry: FlightEntry,
    savedAircraft: List<AircraftProfile>,
    onSave: (FlightEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val airports = remember { AirportDatabase.get(context) }
    var isEditing by remember { mutableStateOf(false) }

    FullScreenSheet(
        title = entry.date.format(titleDateFormat),
        onDismissRequest = onDismiss,
        // Color-coded so the actions read at a glance: blue opens the
        // editor, green closes the preview.
        leadingLabel = "Edit",
        leadingColor = MaterialTheme.colorScheme.primary,
        onLeadingClick = { isEditing = true },
        trailingLabel = "Done",
        trailingColor = Color(0xFF34A853),
        onTrailingClick = onDismiss
    ) { modifier ->
        Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // Aircraft and route facts.
            FormSection(title = "Flight") {
                LabeledRow(
                    "Aircraft",
                    listOf(entry.aircraftRegistration, entry.aircraftType)
                        .filter { it.isNotEmpty() }
                        .joinToString(" · ")
                )
                entry.category?.let { category ->
                    LabeledRow(
                        "Category & Class",
                        entry.aircraftClass
                            ?.let { "${category.displayName} · ${it.displayName}" }
                            ?: category.displayName
                    )
                }
                LabeledRow("Route", entry.routeDescription)
                airports.distanceNM(entry.departureAirport, entry.arrivalAirport)?.let {
                    LabeledRow("Distance", "${it.toInt()} NM")
                }
            }

            // Hour columns; zero columns are omitted (total always shows).
            FormSection(title = "Times") {
                HoursRow("Total Time", entry.totalTime)
                NonZeroHoursRow("PIC", entry.picTime)
                NonZeroHoursRow("SIC", entry.sicTime)
                NonZeroHoursRow("Solo", entry.soloTime)
                NonZeroHoursRow("Cross Country", entry.xcTime)
                NonZeroHoursRow("Night", entry.nightTime)
                NonZeroHoursRow("NVG", entry.nvgTime)
                NonZeroHoursRow("Actual Instrument", entry.actualInstrumentTime)
                NonZeroHoursRow("Simulated Instrument", entry.simulatedInstrumentTime)
            }

            // Takeoff and landing counts; zero counts are omitted.
            val counts = listOf(
                "Day Takeoffs" to entry.dayTakeoffs,
                "Day Landings (full stop)" to entry.dayFullStopLandings,
                "Day Landings (non-full stop)" to entry.dayNonFullStopLandings,
                "Night Takeoffs" to entry.nightTakeoffs,
                "Night Landings (full stop)" to entry.nightFullStopLandings,
                "Night Landings (non-full stop)" to entry.nightNonFullStopLandings
            ).filter { it.second > 0 }
            if (counts.isNotEmpty()) {
                FormSection(title = "Takeoffs & Landings") {
                    counts.forEach { (label, count) -> CountRow(label, count) }
                }
            }

            // Training columns; the whole section disappears when none were logged.
            val trainingTimes = listOf(
                "Dual Given" to entry.dualGivenTime,
                "Dual Received" to entry.dualReceivedTime,
                "Simulator / FTD" to entry.simulatorTime,
                "Ground Training Given" to entry.groundTrainingGivenTime,
                "Ground Training Received" to entry.groundTrainingReceivedTime
            ).filter { it.second > 0 }
            if (trainingTimes.isNotEmpty()) {
                FormSection(title = "Training") {
                    trainingTimes.forEach { (label, hours) -> HoursRow(label, hours) }
                }
            }

            if (entry.notes.isNotEmpty()) {
                FormSection(title = "Notes") {
                    Text(
                        entry.notes,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }

    if (isEditing) {
        FlightEntryFormSheet(
            existing = entry,
            savedAircraft = savedAircraft,
            onSave = onSave,
            onDismiss = { isEditing = false }
        )
    }
}

/** A HoursRow that renders nothing for a zero value. */
@Composable
private fun NonZeroHoursRow(label: String, hours: Double) {
    if (hours > 0) HoursRow(label, hours)
}
