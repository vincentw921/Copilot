package com.vincentwang.copilot.features.aircraft

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vincentwang.copilot.features.logbook.LogbookViewModel
import com.vincentwang.copilot.features.report.SubScreenTopBar
import com.vincentwang.copilot.models.FlightEntry
import com.vincentwang.copilot.models.LogbookStats
import com.vincentwang.copilot.models.LogbookTotals
import com.vincentwang.copilot.ui.components.EmptyState
import com.vincentwang.copilot.ui.components.FormSection
import com.vincentwang.copilot.ui.components.formatHours

/**
 * Flight-time totals broken down per aircraft (registration + type),
 * most-flown first. Ported from AircraftView.swift.
 */
@Composable
fun AircraftScreen(onBack: () -> Unit, model: LogbookViewModel = viewModel()) {
    val items by model.items.collectAsState()

    data class AircraftSummary(
        val registration: String,
        val type: String,
        val totals: LogbookTotals
    )

    val entries = items.map { FlightEntry.fromItem(it) }
    val summaries = entries
        .groupBy { it.aircraftRegistration }
        .map { (registration, flights) ->
            AircraftSummary(
                registration = registration.ifEmpty { "Unknown" },
                type = flights.first().aircraftType,
                totals = LogbookStats.totals(flights)
            )
        }
        .sortedByDescending { it.totals.totalTime }

    Scaffold(
        topBar = { SubScreenTopBar(title = "Aircraft", onBack = onBack) }
    ) { padding ->
        if (summaries.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Filled.Flight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = "No Aircraft Yet",
                description = "Aircraft appear here once you log flights.",
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                FormSection {
                    summaries.forEachIndexed { index, summary ->
                        if (index > 0) HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    summary.registration,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (summary.type.isNotEmpty()) {
                                    Text(
                                        summary.type,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${formatHours(summary.totals.totalTime)} hr",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "${summary.totals.flightCount} flights",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
