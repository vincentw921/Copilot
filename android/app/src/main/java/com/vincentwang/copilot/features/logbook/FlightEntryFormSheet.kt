package com.vincentwang.copilot.features.logbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vincentwang.copilot.data.AircraftProfile
import com.vincentwang.copilot.data.AppPrefs
import com.vincentwang.copilot.models.AircraftCategory
import com.vincentwang.copilot.models.AircraftClass
import com.vincentwang.copilot.models.AirportDatabase
import com.vincentwang.copilot.models.FlightEntry
import com.vincentwang.copilot.ui.components.CategoryClassPickers
import com.vincentwang.copilot.ui.components.CountField
import com.vincentwang.copilot.ui.components.DisclosureGroup
import com.vincentwang.copilot.ui.components.FormRow
import com.vincentwang.copilot.ui.components.FormSection
import com.vincentwang.copilot.ui.components.FormTextField
import com.vincentwang.copilot.ui.components.FullScreenSheet
import com.vincentwang.copilot.ui.components.HoursField
import com.vincentwang.copilot.ui.components.MonthCalendar
import com.vincentwang.copilot.ui.components.PickerRow
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Who acted as required crew, driving automatic PIC/SIC time.
 *  "None" covers flights logging neither (e.g. dual received only). */
private enum class Role(val label: String) { PIC("PIC"), SIC("SIC"), NONE("None") }

private val formDateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

/**
 * Add/edit form presented as a full-screen sheet, ported from
 * FlightEntryFormView.swift.
 *
 * Pass `existing = null` to create a new entry, or an entry to edit it.
 * The form edits a local copy and only persists when the user taps Save.
 *
 * Times are checkbox-driven: the pilot enters the total time once and
 * checks PIC/SIC, Solo, Night, etc. — the matching time columns fill in
 * automatically and can be fine-tuned under "Adjust Times".
 */
@Composable
fun FlightEntryFormSheet(
    existing: FlightEntry?,
    savedAircraft: List<AircraftProfile>,
    onSave: (FlightEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val airports = remember { AirportDatabase.get(context) }
    val nvgLoggingEnabled by AppPrefs.nvgLoggingEnabled.collectAsState()

    // Working copy the form fields bind to.
    var entry by remember { mutableStateOf(existing ?: FlightEntry()) }

    // Derive the checkboxes from the stored times when editing;
    // a brand-new flight defaults to PIC (the common case).
    var role by remember {
        mutableStateOf(
            when {
                (existing?.picTime ?: 0.0) > 0 -> Role.PIC
                (existing?.sicTime ?: 0.0) > 0 -> Role.SIC
                existing == null -> Role.PIC
                else -> Role.NONE
            }
        )
    }
    var isSolo by remember { mutableStateOf((existing?.soloTime ?: 0.0) > 0) }
    var isNight by remember { mutableStateOf((existing?.nightTime ?: 0.0) > 0) }
    var isNVG by remember { mutableStateOf((existing?.nvgTime ?: 0.0) > 0) }
    var isDualGiven by remember { mutableStateOf((existing?.dualGivenTime ?: 0.0) > 0) }
    var isDualReceived by remember { mutableStateOf((existing?.dualReceivedTime ?: 0.0) > 0) }
    var isSimulator by remember { mutableStateOf((existing?.simulatorTime ?: 0.0) > 0) }
    // Whether this flight is logged as cross-country. Auto-checked when
    // the route is ≥ 50 NM (14 CFR 61.1); the pilot can override it.
    var isCrossCountry by remember { mutableStateOf((existing?.xcTime ?: 0.0) > 0) }

    // Selected saved aircraft, or null to enter the aircraft manually.
    var selectedAircraftId by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAdjustTimes by remember { mutableStateOf(false) }
    var showInstrument by remember { mutableStateOf(false) }
    var showTraining by remember { mutableStateOf(false) }

    val routeDistanceNM = airports.distanceNM(entry.departureAirport, entry.arrivalAirport)

    /** Refills every checked time column when the total changes. */
    fun applyAutomaticTimes(total: Double) {
        entry = entry.copy(
            totalTime = total,
            picTime = if (role == Role.PIC) total else entry.picTime,
            sicTime = if (role == Role.SIC) total else entry.sicTime,
            soloTime = if (isSolo) total else entry.soloTime,
            nightTime = if (isNight) total else entry.nightTime,
            nvgTime = if (isNVG) total else entry.nvgTime,
            dualGivenTime = if (isDualGiven) total else entry.dualGivenTime,
            dualReceivedTime = if (isDualReceived) total else entry.dualReceivedTime,
            simulatorTime = if (isSimulator) total else entry.simulatorTime,
            xcTime = if (isCrossCountry) total else entry.xcTime
        )
    }

    /** Re-evaluates the cross-country checkbox from the route distance.
     *  Only flips the toggle when both airports resolve, so a manual
     *  override survives typos. */
    fun updateCrossCountryAutoCheck(departure: String, arrival: String) {
        val distance = airports.distanceNM(departure, arrival) ?: return
        val shouldCheck = distance >= AirportDatabase.CROSS_COUNTRY_THRESHOLD_NM
        isCrossCountry = shouldCheck
        entry = entry.copy(xcTime = if (shouldCheck) entry.totalTime else 0.0)
    }

    /** Copies the aircraft fields out of the selected saved profile.
     *  Switching back to "Enter Manually" clears them for a fresh start. */
    fun fillFromSelectedAircraft(id: String?) {
        selectedAircraftId = id
        val plane = savedAircraft.firstOrNull { it.id == id }
        entry = if (plane == null) {
            entry.copy(
                aircraftRegistration = "", aircraftType = "",
                category = null, aircraftClass = null
            )
        } else {
            entry.copy(
                aircraftRegistration = plane.registration.orEmpty(),
                aircraftType = plane.aircraftType.orEmpty(),
                category = AircraftCategory.fromDisplayName(plane.category),
                aircraftClass = AircraftClass.fromDisplayName(plane.aircraftClass)
            )
        }
    }

    /** The checkboxes are authoritative on save: unchecked columns save as
     *  zero, checked ones keep any fine-tuned value. */
    fun save() {
        var result = entry.copy(
            picTime = if (role == Role.PIC) entry.picTime else 0.0,
            sicTime = if (role == Role.SIC) entry.sicTime else 0.0,
            soloTime = if (isSolo) entry.soloTime else 0.0,
            nightTime = if (isNight) entry.nightTime else 0.0,
            nvgTime = if (nvgLoggingEnabled && !isNVG) 0.0 else entry.nvgTime,
            dualGivenTime = if (isDualGiven) entry.dualGivenTime else 0.0,
            dualReceivedTime = if (isDualReceived) entry.dualReceivedTime else 0.0,
            simulatorTime = if (isSimulator) entry.simulatorTime else 0.0
        )
        result = if (!isCrossCountry) {
            result.copy(xcTime = 0.0)
        } else if (result.xcTime == 0.0) {
            result.copy(xcTime = result.totalTime)
        } else {
            result
        }
        onSave(result)
        onDismiss()
    }

    FullScreenSheet(
        title = if (existing == null) "New Flight" else "Edit Flight",
        onDismissRequest = onDismiss,
        // Color-coded so the actions read at a glance: red discards, blue saves.
        leadingLabel = "Cancel",
        leadingColor = MaterialTheme.colorScheme.error,
        onLeadingClick = onDismiss,
        trailingLabel = "Save",
        trailingEnabled = entry.isValid,
        onTrailingClick = ::save
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // MARK: Flight — date, aircraft, and route (61.51(b)(1)).
            FormSection(title = "Flight") {
                FormRow(modifier = Modifier.clickable { showDatePicker = !showDatePicker }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date", modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                entry.date.format(formDateFormat),
                                color = if (showDatePicker) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                if (showDatePicker) {
                    // Collapses on any day tap, even re-tapping the
                    // selected day, like the iOS custom calendar row.
                    MonthCalendar(
                        selectedDate = entry.date,
                        highlightedDays = emptySet(),
                        onDaySelected = { day ->
                            entry = entry.copy(date = day)
                            showDatePicker = false
                        }
                    )
                }

                // Either pick a saved profile or enter the aircraft manually.
                if (savedAircraft.isNotEmpty()) {
                    PickerRow(
                        label = "Aircraft",
                        selection = savedAircraft
                            .firstOrNull { it.id == selectedAircraftId }?.displayName,
                        options = savedAircraft.map { it.displayName },
                        notSpecifiedLabel = "Enter Manually"
                    ) { selected ->
                        fillFromSelectedAircraft(
                            savedAircraft.firstOrNull { it.displayName == selected }?.id
                        )
                    }
                }
                if (selectedAircraftId == null) {
                    FormTextField(
                        placeholder = "Aircraft Type (e.g. C172)",
                        value = entry.aircraftType,
                        onValueChange = { entry = entry.copy(aircraftType = it) },
                        capitalize = true
                    )
                    FormTextField(
                        placeholder = "Registration (e.g. N12345)",
                        value = entry.aircraftRegistration,
                        onValueChange = { entry = entry.copy(aircraftRegistration = it) },
                        capitalize = true
                    )
                    CategoryClassPickers(
                        category = entry.category,
                        aircraftClass = entry.aircraftClass,
                        onCategoryChange = { entry = entry.copy(category = it) },
                        onClassChange = { entry = entry.copy(aircraftClass = it) }
                    )
                }

                FormTextField(
                    placeholder = "From (e.g. KSNA)",
                    value = entry.departureAirport,
                    onValueChange = {
                        entry = entry.copy(departureAirport = it)
                        updateCrossCountryAutoCheck(it, entry.arrivalAirport)
                    },
                    capitalize = true
                )
                if (isUnknownAirport(airports, entry.departureAirport)) {
                    UnknownAirportWarning(entry.departureAirport)
                }
                FormTextField(
                    placeholder = "To (e.g. KLAX)",
                    value = entry.arrivalAirport,
                    onValueChange = {
                        entry = entry.copy(arrivalAirport = it)
                        updateCrossCountryAutoCheck(entry.departureAirport, it)
                    },
                    capitalize = true
                )
                if (isUnknownAirport(airports, entry.arrivalAirport)) {
                    UnknownAirportWarning(entry.arrivalAirport)
                }
            }

            // MARK: Flight time — total plus auto-filling checkboxes.
            FormSection(
                title = "Flight Time (hours)",
                footer = "Checked times fill in from the total automatically. " +
                    "Expand Adjust Times to log partial hours (e.g. 0.5 night of a 2.0 flight)."
            ) {
                HoursField(label = "Total Time", value = entry.totalTime) {
                    applyAutomaticTimes(it)
                }
                FormRow {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        Role.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = role == option,
                                onClick = {
                                    role = option
                                    entry = entry.copy(
                                        picTime = if (option == Role.PIC) entry.totalTime else 0.0,
                                        sicTime = if (option == Role.SIC) entry.totalTime else 0.0
                                    )
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = Role.entries.size
                                )
                            ) {
                                Text(option.label)
                            }
                        }
                    }
                }
                ToggleRow("Solo", isSolo) {
                    isSolo = it
                    entry = entry.copy(soloTime = if (it) entry.totalTime else 0.0)
                }
                ToggleRow("Night", isNight) {
                    isNight = it
                    entry = entry.copy(nightTime = if (it) entry.totalTime else 0.0)
                }
                if (nvgLoggingEnabled) {
                    ToggleRow("NVG", isNVG) {
                        isNVG = it
                        entry = entry.copy(nvgTime = if (it) entry.totalTime else 0.0)
                    }
                }
                DisclosureGroup(
                    "Adjust Times",
                    expanded = showAdjustTimes,
                    onExpandedChange = { showAdjustTimes = it }
                ) {
                    HoursField("PIC", entry.picTime) { entry = entry.copy(picTime = it) }
                    HoursField("SIC", entry.sicTime) { entry = entry.copy(sicTime = it) }
                    HoursField("Solo", entry.soloTime) { entry = entry.copy(soloTime = it) }
                    HoursField("Night", entry.nightTime) { entry = entry.copy(nightTime = it) }
                    if (nvgLoggingEnabled) {
                        HoursField("NVG", entry.nvgTime) { entry = entry.copy(nvgTime = it) }
                    }
                }
            }

            // MARK: Cross country — distance readout and checkbox.
            FormSection(
                footer = "Checked automatically when the route is 50 NM or more " +
                    "(14 CFR 61.1). You can override it."
            ) {
                if (routeDistanceNM != null) {
                    FormRow {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Route Distance", modifier = Modifier.weight(1f))
                            Text(
                                "${routeDistanceNM.toInt()} NM",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                ToggleRow("Cross Country", isCrossCountry) {
                    isCrossCountry = it
                    entry = entry.copy(xcTime = if (it) entry.totalTime else 0.0)
                }
                if (isCrossCountry) {
                    HoursField("Cross Country Time", entry.xcTime) {
                        entry = entry.copy(xcTime = it)
                    }
                }
            }

            // MARK: Takeoffs and landings (61.57 currency tracking).
            FormSection(title = "Takeoffs") {
                CountField("Day", entry.dayTakeoffs) { entry = entry.copy(dayTakeoffs = it) }
                CountField("Night", entry.nightTakeoffs) { entry = entry.copy(nightTakeoffs = it) }
            }
            // Full-stop and non-full-stop (touch-and-go) are tracked
            // separately because night currency only accepts full stops.
            FormSection(title = "Landings") {
                CountField("Day (full stop)", entry.dayFullStopLandings) {
                    entry = entry.copy(dayFullStopLandings = it)
                }
                CountField("Day (non-full stop)", entry.dayNonFullStopLandings) {
                    entry = entry.copy(dayNonFullStopLandings = it)
                }
                CountField("Night (full stop)", entry.nightFullStopLandings) {
                    entry = entry.copy(nightFullStopLandings = it)
                }
                CountField("Night (non-full stop)", entry.nightNonFullStopLandings) {
                    entry = entry.copy(nightNonFullStopLandings = it)
                }
            }

            // MARK: Instrument (61.51(b)(3)), plus the approach and hold
            // counts that feed 61.57(c) instrument currency on the Report tab.
            FormSection(
                footer = "Approaches and holding procedures count toward " +
                    "61.57(c) instrument currency."
            ) {
                DisclosureGroup(
                    "Instrument",
                    expanded = showInstrument,
                    onExpandedChange = { showInstrument = it }
                ) {
                    HoursField("Actual Instrument", entry.actualInstrumentTime) {
                        entry = entry.copy(actualInstrumentTime = it)
                    }
                    HoursField("Simulated Instrument", entry.simulatedInstrumentTime) {
                        entry = entry.copy(simulatedInstrumentTime = it)
                    }
                    CountField("Approaches", entry.approachCount) {
                        entry = entry.copy(approachCount = it)
                    }
                    CountField("Holds", entry.holdCount) {
                        entry = entry.copy(holdCount = it)
                    }
                }
            }

            // MARK: Training.
            FormSection(
                title = "Training",
                footer = "Checked training times fill in from the total automatically."
            ) {
                ToggleRow("Dual Given", isDualGiven) {
                    isDualGiven = it
                    entry = entry.copy(dualGivenTime = if (it) entry.totalTime else 0.0)
                }
                ToggleRow("Dual Received", isDualReceived) {
                    isDualReceived = it
                    entry = entry.copy(dualReceivedTime = if (it) entry.totalTime else 0.0)
                }
                ToggleRow("Simulator / FTD", isSimulator) {
                    isSimulator = it
                    entry = entry.copy(simulatorTime = if (it) entry.totalTime else 0.0)
                }
                DisclosureGroup(
                    "Adjust Training Times",
                    expanded = showTraining,
                    onExpandedChange = { showTraining = it }
                ) {
                    HoursField("Dual Given", entry.dualGivenTime) {
                        entry = entry.copy(dualGivenTime = it)
                    }
                    HoursField("Dual Received", entry.dualReceivedTime) {
                        entry = entry.copy(dualReceivedTime = it)
                    }
                    HoursField("Simulator / FTD", entry.simulatorTime) {
                        entry = entry.copy(simulatorTime = it)
                    }
                    HoursField("Ground Training Given", entry.groundTrainingGivenTime) {
                        entry = entry.copy(groundTrainingGivenTime = it)
                    }
                    HoursField("Ground Training Received", entry.groundTrainingReceivedTime) {
                        entry = entry.copy(groundTrainingReceivedTime = it)
                    }
                }
            }

            // MARK: Notes.
            FormSection(title = "Notes") {
                FormTextField(
                    placeholder = "Remarks, endorsements…",
                    value = entry.notes,
                    onValueChange = { entry = entry.copy(notes = it) },
                    singleLine = false
                )
            }
        }
    }
}

/** Labeled switch row (Toggle on iOS). */
@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    FormRow {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/** True when `code` is non-empty but not in the airport database —
 *  likely a typo, or a private strip we don't know about. */
private fun isUnknownAirport(airports: AirportDatabase, code: String): Boolean {
    val trimmed = code.trim()
    return trimmed.isNotEmpty() && airports.coordinate(trimmed) == null
}

/** The warning row shown under an unrecognized airport field. */
@Composable
private fun UnknownAirportWarning(code: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = Color(0xFFF29900),
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            "${code.trim().uppercase()} not found — check the identifier",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFF29900)
        )
    }
}

/** Simple confirmation used for save errors (alert on iOS). */
@Composable
fun SaveErrorDialog(message: String?, onDismiss: () -> Unit) {
    if (message != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Could Not Save") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
    }
}
