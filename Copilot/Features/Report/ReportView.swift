//
//  ReportView.swift
//  Copilot
//
//  Career totals, FAR 61.57 currency, per-aircraft breakdown, and
//  CSV export of the complete logbook.
//

import SwiftUI
import CoreData

/// Summary report over the entire logbook.
struct ReportView: View {
    /// Mirrors the Settings toggle; hides the NVG total when disabled.
    @AppStorage("nvgLoggingEnabled") private var nvgLoggingEnabled = false

    /// All entries, newest first.
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Item.date, ascending: false)],
        animation: .default
    ) private var items: FetchedResults<Item>

    /// Value copies of every entry, used for all calculations and export.
    private var entries: [FlightEntry] { items.map(FlightEntry.init) }

    var body: some View {
        let entries = self.entries
        let totals = LogbookStats.totals(for: entries)

        List {
            currencySection(entries: entries)
            timeTotalsSection(totals: totals)
            categorySection(entries: entries)
            landingsSection(totals: totals)
            trainingSection(totals: totals)
            Section {
                NavigationLink("Totals by Aircraft") { AircraftView() }
            }
            exportSection(entries: entries, totals: totals)
        }
        .navigationTitle("Report")
    }

    // MARK: Sections

    /// Passenger-carrying currency per 61.57(a)/(b) over the last 90 days.
    private func currencySection(entries: [FlightEntry]) -> some View {
        let day = LogbookStats.dayCurrency(for: entries)
        let night = LogbookStats.nightCurrency(for: entries)

        return Section {
            CurrencyRow(title: "Day Passengers", status: day)
            CurrencyRow(title: "Night Passengers", status: night)
        } header: {
            Text("Currency (last 90 days)")
        } footer: {
            Text("14 CFR 61.57 requires 3 takeoffs and 3 landings within the preceding 90 days (full-stop at night) to carry passengers.")
        }
    }

    /// Career flight-time totals.
    private func timeTotalsSection(totals: LogbookTotals) -> some View {
        Section("Flight Time") {
            HoursRow(label: "Total Time", hours: totals.totalTime)
            HoursRow(label: "Pilot in Command", hours: totals.picTime)
            HoursRow(label: "Second in Command", hours: totals.sicTime)
            HoursRow(label: "Solo", hours: totals.soloTime)
            HoursRow(label: "Cross Country", hours: totals.xcTime)
            HoursRow(label: "Night", hours: totals.nightTime)
            if nvgLoggingEnabled {
                HoursRow(label: "NVG", hours: totals.nvgTime)
            }
            HoursRow(label: "Actual Instrument", hours: totals.actualInstrumentTime)
            HoursRow(label: "Simulated Instrument", hours: totals.simulatedInstrumentTime)
        }
    }

    /// Time per aircraft category and class. Pairs with zero time (or no
    /// category logged) are omitted entirely.
    @ViewBuilder
    private func categorySection(entries: [FlightEntry]) -> some View {
        let categoryTotals = LogbookStats.categoryTotals(for: entries)
        if !categoryTotals.isEmpty {
            Section("By Category & Class") {
                ForEach(categoryTotals) { total in
                    HoursRow(label: total.label, hours: total.hours)
                }
            }
        }
    }

    /// Career takeoff and landing counts.
    private func landingsSection(totals: LogbookTotals) -> some View {
        Section("Takeoffs & Landings") {
            CountRow(label: "Day Takeoffs", count: totals.dayTakeoffs)
            CountRow(label: "Day Landings (full stop)", count: totals.dayFullStopLandings)
            CountRow(label: "Day Landings (non-full stop)", count: totals.dayNonFullStopLandings)
            CountRow(label: "Night Takeoffs", count: totals.nightTakeoffs)
            CountRow(label: "Night Landings (full stop)", count: totals.nightFullStopLandings)
            CountRow(label: "Night Landings (non-full stop)", count: totals.nightNonFullStopLandings)
        }
    }

    /// Career training totals.
    private func trainingSection(totals: LogbookTotals) -> some View {
        Section("Training") {
            HoursRow(label: "Dual Given", hours: totals.dualGivenTime)
            HoursRow(label: "Dual Received", hours: totals.dualReceivedTime)
            HoursRow(label: "Simulator / FTD", hours: totals.simulatorTime)
            HoursRow(label: "Ground Training Given", hours: totals.groundTrainingGivenTime)
            HoursRow(label: "Ground Training Received", hours: totals.groundTrainingReceivedTime)
        }
    }

    /// Share-sheet export of the full logbook as a spreadsheet-ready CSV.
    private func exportSection(entries: [FlightEntry], totals: LogbookTotals) -> some View {
        Section {
            ShareLink(
                item: LogbookCSVFile(entries: entries),
                preview: SharePreview("Pilot Logbook CSV", image: Image(systemName: "tablecells"))
            ) {
                Label("Export Logbook as CSV", systemImage: "square.and.arrow.up")
            }
            .disabled(entries.isEmpty)
        } footer: {
            Text("\(totals.flightCount) flights · opens in Numbers, Excel, or Google Sheets for saving and printing.")
        }
    }
}

// MARK: - Reusable rows

/// Green/red currency line with the counts backing the verdict.
struct CurrencyRow: View {
    let title: String
    let status: CurrencyStatus

    var body: some View {
        HStack {
            Image(systemName: status.isCurrent ? "checkmark.circle.fill" : "xmark.circle.fill")
                .foregroundStyle(status.isCurrent ? .green : .red)
            Text(title)
            Spacer()
            Text("\(status.takeoffs) T/O · \(status.landings) LDG")
                .foregroundStyle(.secondary)
                .monospacedDigit()
        }
    }
}

/// Label with right-aligned decimal hours.
struct HoursRow: View {
    let label: String
    let hours: Double

    var body: some View {
        HStack {
            Text(label)
            Spacer()
            Text(hours, format: .number.precision(.fractionLength(1)))
                .monospacedDigit()
                .foregroundStyle(.secondary)
        }
    }
}

/// Label with a right-aligned whole-number count.
struct CountRow: View {
    let label: String
    let count: Int

    var body: some View {
        HStack {
            Text(label)
            Spacer()
            Text("\(count)")
                .monospacedDigit()
                .foregroundStyle(.secondary)
        }
    }
}

#Preview {
    NavigationStack {
        ReportView()
    }
    .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
