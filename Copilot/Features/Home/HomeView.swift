//
//  HomeView.swift
//  Copilot
//
//  Dashboard: headline totals, currency at a glance, and the most
//  recent flights, with a shortcut to log a new one.
//

import SwiftUI
import CoreData

/// Landing screen shown in the first tab.
struct HomeView: View {
    /// All entries, newest first.
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Item.date, ascending: false)],
        animation: .default
    ) private var items: FetchedResults<Item>

    /// True while the "New Flight" sheet is presented.
    @State private var isAddingFlight = false

    var body: some View {
        let entries = items.map(FlightEntry.init)
        let totals = LogbookStats.totals(for: entries)

        List {
            statsSection(totals: totals)
            currencySection(entries: entries)
            recentFlightsSection
        }
        .navigationTitle("Copilot")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    isAddingFlight = true
                } label: {
                    Label("New Flight", systemImage: "plus")
                }
            }
        }
        .sheet(isPresented: $isAddingFlight) {
            FlightEntryFormView()
        }
    }

    /// Headline career numbers.
    private func statsSection(totals: LogbookTotals) -> some View {
        Section {
            HStack {
                StatTile(value: totals.totalTime, format: .hours, caption: "Total Hours")
                StatTile(value: totals.picTime, format: .hours, caption: "PIC Hours")
                StatTile(value: Double(totals.flightCount), format: .count, caption: "Flights")
            }
            .listRowInsets(EdgeInsets(top: 12, leading: 8, bottom: 12, trailing: 8))
            .listRowBackground(Color.clear)
        }
    }

    /// One-line day/night currency check (details live in the Report tab).
    private func currencySection(entries: [FlightEntry]) -> some View {
        Section("Currency") {
            CurrencyRow(title: "Day Passengers", status: LogbookStats.dayCurrency(for: entries))
            CurrencyRow(title: "Night Passengers", status: LogbookStats.nightCurrency(for: entries))
        }
    }

    /// The five most recent flights.
    private var recentFlightsSection: some View {
        Section("Recent Flights") {
            if items.isEmpty {
                Text("No flights yet — tap + to log your first one.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(items.prefix(5)) { item in
                    FlightRow(item: item)
                }
            }
        }
    }
}

/// Big-number dashboard tile.
struct StatTile: View {
    /// How the tile's number is rendered.
    enum Format {
        /// One decimal place (flight hours).
        case hours
        /// Whole number (flight count).
        case count
    }

    let value: Double
    let format: Format
    let caption: String

    var body: some View {
        VStack(spacing: 4) {
            Text(value, format: .number.precision(.fractionLength(format == .hours ? 1 : 0)))
                .font(.title2.bold())
                .monospacedDigit()
            Text(caption)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(.fill.tertiary, in: RoundedRectangle(cornerRadius: 12))
    }
}

#Preview {
    NavigationStack {
        HomeView()
    }
    .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
