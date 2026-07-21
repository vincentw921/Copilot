package com.vincentwang.copilot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vincentwang.copilot.models.CurrencyStatus
import com.vincentwang.copilot.models.FlightEntry
import com.vincentwang.copilot.models.InstrumentCurrencyStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rowDateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

/** One line of the logbook: date and hours up top, aircraft and route
 *  below (FlightRow on iOS). */
@Composable
fun FlightRow(entry: FlightEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                entry.date.format(rowDateFormat),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${formatHours(entry.totalTime)} hr",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(
                listOf(entry.aircraftRegistration, entry.aircraftType)
                    .filter { it.isNotEmpty() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                entry.routeDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Green/red currency line with the counts backing the verdict
 *  (CurrencyRow on iOS). */
@Composable
fun CurrencyRow(title: String, status: CurrencyStatus) {
    FormRow {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (status.isCurrent) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = if (status.isCurrent) "Current" else "Not current",
                tint = if (status.isCurrent) Color(0xFF34A853) else MaterialTheme.colorScheme.error
            )
            Text(title, modifier = Modifier.weight(1f).padding(start = 8.dp))
            Text(
                "${status.takeoffs} T/O · ${status.landings} LDG",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Green/red instrument-currency line: approach count plus whether
 *  holding procedures were logged within the window
 *  (InstrumentCurrencyRow on iOS). */
@Composable
fun InstrumentCurrencyRow(status: InstrumentCurrencyStatus) {
    FormRow {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (status.isCurrent) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = if (status.isCurrent) "Current" else "Not current",
                tint = if (status.isCurrent) Color(0xFF34A853) else MaterialTheme.colorScheme.error
            )
            Text("Instrument", modifier = Modifier.weight(1f).padding(start = 8.dp))
            Text(
                "${status.approaches} APP · ${status.holds} HOLD",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
