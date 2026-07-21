package com.vincentwang.copilot.features.report

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vincentwang.copilot.data.AppPrefs
import com.vincentwang.copilot.features.aircraft.AircraftScreen
import com.vincentwang.copilot.features.logbook.LogbookViewModel
import com.vincentwang.copilot.models.CsvExporter
import com.vincentwang.copilot.models.FlightEntry
import com.vincentwang.copilot.models.LogbookStats
import com.vincentwang.copilot.ui.components.CountRow
import com.vincentwang.copilot.ui.components.CurrencyRow
import com.vincentwang.copilot.ui.components.FormRow
import com.vincentwang.copilot.ui.components.FormSection
import com.vincentwang.copilot.ui.components.HoursRow
import com.vincentwang.copilot.ui.components.InstrumentCurrencyRow

/**
 * Career totals, FAR 61.57 currency, per-aircraft breakdown, and CSV
 * export of the complete logbook. Ported from ReportView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(model: LogbookViewModel = viewModel()) {
    val context = LocalContext.current
    val items by model.items.collectAsState()
    val nvgLoggingEnabled by AppPrefs.nvgLoggingEnabled.collectAsState()

    // Simple push navigation to the per-aircraft breakdown.
    var showingAircraft by remember { mutableStateOf(false) }
    if (showingAircraft) {
        BackHandler { showingAircraft = false }
        AircraftScreen(onBack = { showingAircraft = false })
        return
    }

    val entries = items.map { FlightEntry.fromItem(it) }
    val totals = LogbookStats.totals(entries)
    val day = LogbookStats.dayCurrency(entries)
    val night = LogbookStats.nightCurrency(entries)
    val instrument = LogbookStats.instrumentCurrency(entries)
    val categoryTotals = LogbookStats.categoryTotals(entries)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Report") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Passenger-carrying currency per 61.57(a)/(b) over the last 90 days,
            // plus instrument currency per 61.57(c) over the last 6 months.
            FormSection(
                title = "Currency",
                footer = "14 CFR 61.57 requires 3 takeoffs and 3 landings within the " +
                    "preceding 90 days (full-stop at night) to carry passengers, and " +
                    "6 approaches plus holding procedures within the preceding " +
                    "6 months to fly in instrument conditions."
            ) {
                CurrencyRow("Day Passengers", day)
                CurrencyRow("Night Passengers", night)
                InstrumentCurrencyRow(instrument)
            }

            // Career flight-time totals.
            FormSection(title = "Flight Time") {
                HoursRow("Total Time", totals.totalTime)
                HoursRow("Pilot in Command", totals.picTime)
                HoursRow("Second in Command", totals.sicTime)
                HoursRow("Solo", totals.soloTime)
                HoursRow("Cross Country", totals.xcTime)
                HoursRow("Night", totals.nightTime)
                if (nvgLoggingEnabled) {
                    HoursRow("NVG", totals.nvgTime)
                }
                HoursRow("Actual Instrument", totals.actualInstrumentTime)
                HoursRow("Simulated Instrument", totals.simulatedInstrumentTime)
            }

            // Time per aircraft category and class. Pairs with zero time
            // (or no category logged) are omitted entirely.
            if (categoryTotals.isNotEmpty()) {
                FormSection(title = "By Category & Class") {
                    categoryTotals.forEach { total ->
                        HoursRow(total.label, total.hours)
                    }
                }
            }

            // Career takeoff and landing counts.
            FormSection(title = "Takeoffs & Landings") {
                CountRow("Day Takeoffs", totals.dayTakeoffs)
                CountRow("Day Landings (full stop)", totals.dayFullStopLandings)
                CountRow("Day Landings (non-full stop)", totals.dayNonFullStopLandings)
                CountRow("Night Takeoffs", totals.nightTakeoffs)
                CountRow("Night Landings (full stop)", totals.nightFullStopLandings)
                CountRow("Night Landings (non-full stop)", totals.nightNonFullStopLandings)
            }

            // Career training totals.
            FormSection(title = "Training") {
                HoursRow("Dual Given", totals.dualGivenTime)
                HoursRow("Dual Received", totals.dualReceivedTime)
                HoursRow("Simulator / FTD", totals.simulatorTime)
                HoursRow("Ground Training Given", totals.groundTrainingGivenTime)
                HoursRow("Ground Training Received", totals.groundTrainingReceivedTime)
            }

            FormSection {
                FormRow(modifier = Modifier.clickable { showingAircraft = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Totals by Aircraft", modifier = Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Share-sheet export of the full logbook as a spreadsheet-ready CSV.
            FormSection(
                footer = "${totals.flightCount} flights · opens in Sheets or Excel " +
                    "for saving and printing."
            ) {
                FormRow(
                    modifier = Modifier.clickable(enabled = entries.isNotEmpty()) {
                        shareCsv(context, entries)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = null,
                            tint = if (entries.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            "Export Logbook as CSV",
                            color = if (entries.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/** Writes the CSV and hands it to the system share sheet via FileProvider
 *  (the Android counterpart of ShareLink + exportFile on iOS). */
private fun shareCsv(context: android.content.Context, entries: List<FlightEntry>) {
    val file = CsvExporter.exportFile(context, entries)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Logbook"))
}

/** Back-arrow top bar used by sub-screens pushed from a tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}
