//
//  FlightDetailView.swift
//  Copilot
//
//  Read-only preview of one logbook entry, shown when a flight is tapped
//  in the Logbook or Calendar. Nothing here is editable — the Edit button
//  opens the form, and Done closes the preview.
//

import SwiftUI
import CoreData

/// Read-only detail sheet for a single flight.
struct FlightDetailView: View {
    @Environment(\.dismiss) private var dismiss

    /// Observed so the preview refreshes after an edit is saved.
    @ObservedObject var item: Item

    /// True while the edit form is presented on top of the preview.
    @State private var isEditing = false

    var body: some View {
        // A fresh value copy each render keeps the rows in sync with edits.
        let entry = FlightEntry(item: item)

        NavigationStack {
            List {
                flightSection(entry: entry)
                timesSection(entry: entry)
                takeoffsAndLandingsSection(entry: entry)
                trainingSection(entry: entry)
                if !entry.notes.isEmpty {
                    Section("Notes") {
                        Text(entry.notes)
                    }
                }
            }
            .navigationTitle(entry.date.formatted(date: .abbreviated, time: .omitted))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Edit") { isEditing = true }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(isPresented: $isEditing) {
                FlightEntryFormView(item: item)
            }
        }
    }

    // MARK: Sections

    /// Aircraft and route facts.
    private func flightSection(entry: FlightEntry) -> some View {
        Section("Flight") {
            LabeledContent("Aircraft", value: [entry.aircraftRegistration, entry.aircraftType]
                .filter { !$0.isEmpty }
                .joined(separator: " · "))
            if let category = entry.category {
                LabeledContent(
                    "Category & Class",
                    value: entry.aircraftClass.map { "\(category.rawValue) · \($0.rawValue)" }
                        ?? category.rawValue
                )
            }
            LabeledContent("Route", value: entry.routeDescription)
            if let distance = AirportDatabase.shared.distanceNM(
                from: entry.departureAirport, to: entry.arrivalAirport
            ) {
                LabeledContent(
                    "Distance",
                    value: "\(distance.formatted(.number.precision(.fractionLength(0)))) NM"
                )
            }
        }
    }

    /// Hour columns; zero columns are omitted (total always shows).
    private func timesSection(entry: FlightEntry) -> some View {
        Section("Times") {
            HoursRow(label: "Total Time", hours: entry.totalTime)
            nonZeroHoursRow("PIC", entry.picTime)
            nonZeroHoursRow("SIC", entry.sicTime)
            nonZeroHoursRow("Solo", entry.soloTime)
            nonZeroHoursRow("Cross Country", entry.xcTime)
            nonZeroHoursRow("Night", entry.nightTime)
            nonZeroHoursRow("NVG", entry.nvgTime)
            nonZeroHoursRow("Actual Instrument", entry.actualInstrumentTime)
            nonZeroHoursRow("Simulated Instrument", entry.simulatedInstrumentTime)
        }
    }

    /// Takeoff and landing counts; zero counts are omitted.
    @ViewBuilder
    private func takeoffsAndLandingsSection(entry: FlightEntry) -> some View {
        let counts: [(String, Int)] = [
            ("Day Takeoffs", entry.dayTakeoffs),
            ("Day Landings (full stop)", entry.dayFullStopLandings),
            ("Day Landings (non-full stop)", entry.dayNonFullStopLandings),
            ("Night Takeoffs", entry.nightTakeoffs),
            ("Night Landings (full stop)", entry.nightFullStopLandings),
            ("Night Landings (non-full stop)", entry.nightNonFullStopLandings),
        ].filter { $0.1 > 0 }

        if !counts.isEmpty {
            Section("Takeoffs & Landings") {
                ForEach(counts, id: \.0) { label, count in
                    CountRow(label: label, count: count)
                }
            }
        }
    }

    /// Training columns; the whole section disappears when none were logged.
    @ViewBuilder
    private func trainingSection(entry: FlightEntry) -> some View {
        let times: [(String, Double)] = [
            ("Dual Given", entry.dualGivenTime),
            ("Dual Received", entry.dualReceivedTime),
            ("Simulator / FTD", entry.simulatorTime),
            ("Ground Training Given", entry.groundTrainingGivenTime),
            ("Ground Training Received", entry.groundTrainingReceivedTime),
        ].filter { $0.1 > 0 }

        if !times.isEmpty {
            Section("Training") {
                ForEach(times, id: \.0) { label, hours in
                    HoursRow(label: label, hours: hours)
                }
            }
        }
    }

    /// A HoursRow that renders nothing for a zero value.
    @ViewBuilder
    private func nonZeroHoursRow(_ label: String, _ hours: Double) -> some View {
        if hours > 0 {
            HoursRow(label: label, hours: hours)
        }
    }
}

#Preview {
    let context = PersistenceController.preview.container.viewContext
    let item = try! context.fetch(Item.fetchRequest()).first!
    return FlightDetailView(item: item)
        .environment(\.managedObjectContext, context)
}
