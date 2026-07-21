package com.vincentwang.copilot.models

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns logbook entries into a spreadsheet-ready CSV file that can be
 * shared through the Android share sheet. Ported from CSVExporter.swift.
 */
object CsvExporter {
    /** Column headers, in the order columns appear in the export. */
    val headers = listOf(
        "Date", "Aircraft Type", "Category", "Class", "Registration", "From", "To",
        "Total Time", "PIC", "SIC", "Solo", "Cross Country", "Night", "NVG",
        "Actual Instrument", "Simulated Instrument", "Approaches", "Holds",
        "Day Takeoffs", "Day Full-Stop Landings", "Day Non-Full-Stop Landings",
        "Night Takeoffs", "Night Full-Stop Landings", "Night Non-Full-Stop Landings",
        "Dual Given", "Dual Received", "Simulator",
        "Ground Training Given", "Ground Training Received",
        "Notes"
    )

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Renders `entries` as CSV, one row per flight, oldest first
     *  (the conventional order of a paper logbook). */
    fun csv(entries: List<FlightEntry>): String {
        val rows = mutableListOf(headers.joinToString(",", transform = ::escape))
        for (e in entries.sortedBy { it.date }) {
            val fields = listOf(
                e.date.format(dateFormat),
                e.aircraftType,
                e.category?.displayName.orEmpty(), e.aircraftClass?.displayName.orEmpty(),
                e.aircraftRegistration,
                e.departureAirport, e.arrivalAirport,
                hours(e.totalTime), hours(e.picTime), hours(e.sicTime),
                hours(e.soloTime), hours(e.xcTime), hours(e.nightTime), hours(e.nvgTime),
                hours(e.actualInstrumentTime), hours(e.simulatedInstrumentTime),
                e.approachCount.toString(), e.holdCount.toString(),
                e.dayTakeoffs.toString(), e.dayFullStopLandings.toString(),
                e.dayNonFullStopLandings.toString(),
                e.nightTakeoffs.toString(), e.nightFullStopLandings.toString(),
                e.nightNonFullStopLandings.toString(),
                hours(e.dualGivenTime), hours(e.dualReceivedTime), hours(e.simulatorTime),
                hours(e.groundTrainingGivenTime), hours(e.groundTrainingReceivedTime),
                e.notes
            )
            rows.add(fields.joinToString(",", transform = ::escape))
        }
        return rows.joinToString("\n") + "\n"
    }

    /** Writes `entries` as a CSV file in the cache directory and returns
     *  it, e.g. …/Logbook-2026-07-20.csv. The file is only rewritten when
     *  the contents changed. Shared through FileProvider. */
    fun exportFile(
        context: Context,
        entries: List<FlightEntry>,
        date: LocalDate = LocalDate.now()
    ): File {
        val file = File(context.cacheDir, "Logbook-${date.format(dateFormat)}.csv")
        val contents = csv(entries)
        if (!file.exists() || file.readText() != contents) {
            file.writeText(contents)
        }
        return file
    }

    /** Formats decimal hours with one fractional digit ("1.5", "0.0"). */
    private fun hours(value: Double): String = String.format(Locale.US, "%.1f", value)

    /** Quotes a field when it contains a comma, quote, or newline (RFC 4180). */
    private fun escape(field: String): String {
        if (field.none { it in ",\"\n\r" }) return field
        return "\"" + field.replace("\"", "\"\"") + "\""
    }
}
