//
//  ProfileView.swift
//  Copilot
//
//  Pilot identity, iCloud account status, and (debug builds only)
//  sample-data utilities.
//

import SwiftUI
import CoreData

/// Settings/profile tab.
struct ProfileView: View {
    @EnvironmentObject private var auth: AuthModel
    @Environment(\.managedObjectContext) private var viewContext

    // Pilot identity is device-local preference data, not flight records,
    // so plain AppStorage is sufficient.
    /// Pilot's display name, shown on exports and reports.
    @AppStorage("pilotName") private var pilotName = ""
    /// FAA certificate number (optional).
    @AppStorage("certificateNumber") private var certificateNumber = ""

    var body: some View {
        Form {
            pilotSection
            aircraftSection
            accountSection
            #if DEBUG
            developerSection
            #endif
        }
        .navigationTitle("Profile")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                NavigationLink {
                    SettingsView()
                } label: {
                    Label("Settings", systemImage: "gearshape")
                }
            }
        }
    }

    /// Name and certificate number.
    private var pilotSection: some View {
        Section("Pilot") {
            TextField("Name", text: $pilotName)
                .textContentType(.name)
            TextField("Certificate Number", text: $certificateNumber)
                .autocorrectionDisabled()
        }
    }

    /// Saved aircraft profiles (optional shortcuts for the flight form).
    private var aircraftSection: some View {
        Section {
            NavigationLink {
                SavedAircraftView()
            } label: {
                Label("My Aircraft", systemImage: "airplane.circle")
            }
        } header: {
            Text("Aircraft")
        } footer: {
            Text("Save aircraft you fly often so the flight form can fill in the registration and type for you.")
        }
    }

    /// iCloud sync status and local sign-out.
    private var accountSection: some View {
        Section {
            switch auth.state {
            case .signedIn:
                Label("Synced with iCloud", systemImage: "checkmark.icloud")
                Button("Sign Out on This Device", role: .destructive) {
                    auth.signOutLocally()
                }
            case .signedOut:
                Label("Not signed into iCloud", systemImage: "xmark.icloud")
            case .idle, .checking:
                Label("Checking iCloud…", systemImage: "arrow.triangle.2.circlepath.icloud")
            case .error(let message):
                Label(message, systemImage: "exclamationmark.icloud")
            }
        } header: {
            Text("iCloud")
        } footer: {
            Text("Your logbook is stored in your personal iCloud and syncs across your devices automatically.")
        }
    }

    #if DEBUG
    /// Sample-data helpers for development builds.
    private var developerSection: some View {
        Section("Developer") {
            Button("Insert Sample Flights") { insertSampleData() }
            Button("Delete All Flights", role: .destructive) { deleteAllData() }
        }
    }

    /// Seeds a varied set of realistic entries — different categories and
    /// classes (SEL, MEL, SES, helicopter, glider), a cross-country leg,
    /// and a night flight — so every screen has something to show.
    private func insertSampleData() {
        /// One sample flight's fixed characteristics.
        struct Sample {
            let type: String
            let registration: String
            let category: AircraftCategory
            let aircraftClass: AircraftClass?
            let from: String
            let to: String
            let hours: Double
            let isNight: Bool
        }

        let samples: [Sample] = [
            .init(type: "C172", registration: "N735AB", category: .airplane,
                  aircraftClass: .singleEngineLand, from: "KSNA", to: "KLAX",
                  hours: 1.1, isNight: false),
            .init(type: "BE76", registration: "N4478C", category: .airplane,
                  aircraftClass: .multiEngineLand, from: "KSNA", to: "KSBP",
                  hours: 2.4, isNight: false),
            .init(type: "ICON A5", registration: "N882BA", category: .airplane,
                  aircraftClass: .singleEngineSea, from: "KAVX", to: "KSNA",
                  hours: 1.3, isNight: false),
            .init(type: "R44", registration: "N7204Q", category: .rotorcraft,
                  aircraftClass: .helicopter, from: "KSNA", to: "KSNA",
                  hours: 0.8, isNight: false),
            .init(type: "ASK 21", registration: "N321GL", category: .glider,
                  aircraftClass: nil, from: "KHMT", to: "KHMT",
                  hours: 0.9, isNight: false),
            .init(type: "C172", registration: "N735AB", category: .airplane,
                  aircraftClass: .singleEngineLand, from: "KSNA", to: "KCRQ",
                  hours: 1.5, isNight: true),
        ]

        for (index, sample) in samples.enumerated() {
            var entry = FlightEntry()
            entry.date = Calendar.current.date(byAdding: .day, value: -index * 9, to: .now) ?? .now
            entry.aircraftType = sample.type
            entry.aircraftRegistration = sample.registration
            entry.category = sample.category
            entry.aircraftClass = sample.aircraftClass
            entry.departureAirport = sample.from
            entry.arrivalAirport = sample.to
            entry.totalTime = sample.hours
            entry.picTime = sample.hours

            // Log cross-country time the same way the form would.
            if let distance = AirportDatabase.shared.distanceNM(from: sample.from, to: sample.to),
               distance >= AirportDatabase.crossCountryThresholdNM {
                entry.xcTime = sample.hours
            }

            if sample.isNight {
                entry.nightTime = sample.hours
                entry.nightTakeoffs = 3
                entry.nightFullStopLandings = 3
            } else {
                // Pattern work: every takeoff but the last is a touch-and-go.
                entry.dayTakeoffs = Int.random(in: 1...3)
                entry.dayFullStopLandings = 1
                entry.dayNonFullStopLandings = entry.dayTakeoffs - 1
            }

            entry.notes = "Sample flight"
            entry.apply(to: Item(context: viewContext))
        }
        try? viewContext.save()
    }

    /// Removes every logbook entry.
    private func deleteAllData() {
        let request: NSFetchRequest<Item> = Item.fetchRequest()
        if let all = try? viewContext.fetch(request) {
            all.forEach(viewContext.delete)
            try? viewContext.save()
        }
    }
    #endif
}

#Preview {
    NavigationStack {
        ProfileView()
    }
    .environmentObject(AuthModel())
    .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
