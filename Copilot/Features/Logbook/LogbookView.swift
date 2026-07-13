//
//  LogbookView.swift
//  Copilot
//
//  The main logbook: every flight, newest first, grouped by month.
//  Tap a row to edit, swipe to delete, "+" to log a new flight.
//

import SwiftUI
import CoreData

/// Scrollable, searchable list of all logbook entries.
struct LogbookView: View {
    @Environment(\.managedObjectContext) private var viewContext

    /// All entries, newest first. CloudKit changes merge in automatically.
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Item.date, ascending: false)],
        animation: .default
    ) private var items: FetchedResults<Item>

    /// Text typed into the search bar.
    @State private var searchText = ""
    /// Non-nil while the "New Flight" sheet is up.
    @State private var isAddingFlight = false
    /// Entry currently open in the read-only preview sheet.
    @State private var viewingItem: Item?

    var body: some View {
        Group {
            if items.isEmpty {
                ContentUnavailableView(
                    "No Flights Logged",
                    systemImage: "airplane",
                    description: Text("Tap + to log your first flight.")
                )
            } else {
                logbookList
            }
        }
        .navigationTitle("Logbook")
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
        .sheet(item: $viewingItem) { item in
            FlightDetailView(item: item)
        }
    }

    /// The grouped list, filtered by the search text.
    private var logbookList: some View {
        List {
            ForEach(monthSections, id: \.title) { section in
                Section(section.title) {
                    ForEach(section.items) { item in
                        Button {
                            viewingItem = item
                        } label: {
                            FlightRow(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                    .onDelete { offsets in
                        delete(offsets.map { section.items[$0] })
                    }
                }
            }
        }
        .searchable(text: $searchText, prompt: "Registration, type, or airport")
    }

    /// Entries matching the search text (all entries when the search is empty).
    private var filteredItems: [Item] {
        let query = searchText.trimmingCharacters(in: .whitespaces)
        guard !query.isEmpty else { return Array(items) }
        return items.filter { item in
            [item.aircraftRegistration, item.aircraftType,
             item.departureAirport, item.arrivalAirport]
                .compactMap { $0 }
                .contains { $0.localizedCaseInsensitiveContains(query) }
        }
    }

    /// Consecutive month groups ("July 2026") built from the date-sorted list.
    private var monthSections: [(title: String, items: [Item])] {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"

        var sections: [(title: String, items: [Item])] = []
        for item in filteredItems {
            let title = formatter.string(from: item.date ?? .distantPast)
            if sections.last?.title == title {
                sections[sections.count - 1].items.append(item)
            } else {
                sections.append((title, [item]))
            }
        }
        return sections
    }

    /// Removes the given entries and saves; CloudKit picks up the deletion.
    private func delete(_ itemsToDelete: [Item]) {
        itemsToDelete.forEach(viewContext.delete)
        do {
            try viewContext.save()
        } catch {
            viewContext.rollback()
        }
    }
}

/// One line of the logbook: date and hours up top, aircraft and route below.
struct FlightRow: View {
    @ObservedObject var item: Item

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(item.date ?? Date(), format: .dateTime.month(.abbreviated).day().year())
                    .font(.headline)
                Spacer()
                Text("\(item.totalTime, format: .number.precision(.fractionLength(1))) hr")
                    .font(.headline)
                    .monospacedDigit()
            }
            HStack {
                Text([item.aircraftRegistration, item.aircraftType]
                    .compactMap { $0 }
                    .filter { !$0.isEmpty }
                    .joined(separator: " · "))
                Spacer()
                Text(FlightEntry(item: item).routeDescription)
            }
            .font(.subheadline)
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }
}

#Preview {
    NavigationStack {
        LogbookView()
    }
    .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
