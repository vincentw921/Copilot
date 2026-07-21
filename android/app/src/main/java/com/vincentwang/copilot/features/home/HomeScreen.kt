package com.vincentwang.copilot.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vincentwang.copilot.features.logbook.FlightEntryFormSheet
import com.vincentwang.copilot.features.logbook.LogbookViewModel
import com.vincentwang.copilot.models.FlightEntry
import com.vincentwang.copilot.models.LogbookStats
import com.vincentwang.copilot.ui.components.CurrencyRow
import com.vincentwang.copilot.ui.components.FlightRow
import com.vincentwang.copilot.ui.components.FormRow
import com.vincentwang.copilot.ui.components.FormSection
import com.vincentwang.copilot.ui.components.formatHours

/**
 * Dashboard: headline totals, currency at a glance, and the most recent
 * flights, with a shortcut to log a new one. Ported from HomeView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(model: LogbookViewModel = viewModel()) {
    val items by model.items.collectAsState()
    val savedAircraft by model.aircraft.collectAsState()
    var isAddingFlight by remember { mutableStateOf(false) }

    val entries = items.map { FlightEntry.fromItem(it) }
    val totals = LogbookStats.totals(entries)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Copilot") },
                actions = {
                    IconButton(onClick = { isAddingFlight = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New Flight")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Headline career numbers.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(formatHours(totals.totalTime), "Total Hours", Modifier.weight(1f))
                StatTile(formatHours(totals.picTime), "PIC Hours", Modifier.weight(1f))
                StatTile("${totals.flightCount}", "Flights", Modifier.weight(1f))
            }

            // One-line day/night currency check (details live in the Report tab).
            FormSection(title = "Currency") {
                CurrencyRow("Day Passengers", LogbookStats.dayCurrency(entries))
                CurrencyRow("Night Passengers", LogbookStats.nightCurrency(entries))
            }

            // The five most recent flights.
            FormSection(title = "Recent Flights") {
                if (entries.isEmpty()) {
                    FormRow {
                        Text(
                            "No flights yet — tap + to log your first one.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    entries.take(5).forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider()
                        FlightRow(entry)
                    }
                }
            }
        }
    }

    if (isAddingFlight) {
        FlightEntryFormSheet(
            existing = null,
            savedAircraft = savedAircraft,
            onSave = model::saveEntry,
            onDismiss = { isAddingFlight = false }
        )
    }
}

/** Big-number dashboard tile (StatTile on iOS). */
@Composable
private fun StatTile(value: String, caption: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
