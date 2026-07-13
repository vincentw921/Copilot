//
//  AircraftView.swift
//  Copilot
//
//  Flight-time totals broken down per aircraft (registration + type).
//

import SwiftUI
import CoreData

/// Hours and landings grouped by individual aircraft, most-flown first.
struct AircraftView: View {
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Item.date, ascending: false)],
        animation: .default
    ) private var items: FetchedResults<Item>

    /// Totals for one aircraft.
    private struct AircraftSummary: Identifiable {
        var id: String { registration }
        let registration: String
        let type: String
        let totals: LogbookTotals
    }

    /// Per-registration summaries, sorted by total time flown (descending).
    private var summaries: [AircraftSummary] {
        let entries = items.map(FlightEntry.init)
        let grouped = Dictionary(grouping: entries) { $0.aircraftRegistration }
        return grouped
            .map { registration, flights in
                AircraftSummary(
                    registration: registration.isEmpty ? "Unknown" : registration,
                    type: flights.first?.aircraftType ?? "",
                    totals: LogbookStats.totals(for: flights)
                )
            }
            .sorted { $0.totals.totalTime > $1.totals.totalTime }
    }

    var body: some View {
        List {
            if summaries.isEmpty {
                ContentUnavailableView(
                    "No Aircraft Yet",
                    systemImage: "airplane",
                    description: Text("Aircraft appear here once you log flights.")
                )
            } else {
                ForEach(summaries) { summary in
                    row(for: summary)
                }
            }
        }
        .navigationTitle("Aircraft")
    }

    /// One aircraft's registration, type, hours, and flight count.
    private func row(for summary: AircraftSummary) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(summary.registration).font(.headline)
                if !summary.type.isEmpty {
                    Text(summary.type)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text("\(summary.totals.totalTime, format: .number.precision(.fractionLength(1))) hr")
                    .font(.headline)
                    .monospacedDigit()
                Text("\(summary.totals.flightCount) flights")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 2)
    }
}

#Preview {
    NavigationStack {
        AircraftView()
    }
    .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
