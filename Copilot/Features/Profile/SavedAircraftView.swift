//
//  SavedAircraftView.swift
//  Copilot
//
//  Management screen for saved aircraft profiles. A profile stores the
//  registration and type once so the flight form can fill them in with
//  one tap. Profiles are entirely optional — the form always accepts
//  free-typed aircraft too.
//

import SwiftUI
import CoreData

extension Aircraft {
    /// The label shown wherever a profile is listed: the custom name when
    /// the pilot set one, otherwise the registration.
    var displayName: String {
        if let name, !name.isEmpty { return name }
        return registration ?? "—"
    }
}

/// List of saved aircraft profiles with add, edit, and delete.
struct SavedAircraftView: View {
    @Environment(\.managedObjectContext) private var viewContext

    /// Saved profiles, alphabetical by registration.
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Aircraft.registration, ascending: true)],
        animation: .default
    ) private var aircraft: FetchedResults<Aircraft>

    /// True while the "add" sheet is presented.
    @State private var isAdding = false
    /// Profile currently open in the edit sheet.
    @State private var editingAircraft: Aircraft?

    var body: some View {
        Group {
            if aircraft.isEmpty {
                ContentUnavailableView(
                    "No Saved Aircraft",
                    systemImage: "airplane.circle",
                    description: Text("Save an aircraft you fly often and the flight form can fill it in for you.")
                )
            } else {
                List {
                    ForEach(aircraft) { plane in
                        Button {
                            editingAircraft = plane
                        } label: {
                            row(for: plane)
                        }
                        .buttonStyle(.plain)
                    }
                    .onDelete(perform: delete)
                }
            }
        }
        .navigationTitle("My Aircraft")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    isAdding = true
                } label: {
                    Label("Add Aircraft", systemImage: "plus")
                }
            }
        }
        .sheet(isPresented: $isAdding) {
            AircraftFormView()
        }
        .sheet(item: $editingAircraft) { plane in
            AircraftFormView(aircraft: plane)
        }
    }

    /// Display name on top; registration, type, and class below.
    private func row(for plane: Aircraft) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(plane.displayName).font(.headline)
            let details = [plane.registration, plane.aircraftType, plane.aircraftClass ?? plane.category]
                .compactMap { $0 }
                .filter { !$0.isEmpty }
                .joined(separator: " · ")
            if !details.isEmpty {
                Text(details)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 2)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
    }

    /// Deletes the swiped profiles. Logged flights keep their own copy of
    /// the aircraft fields, so deleting a profile never touches the logbook.
    private func delete(at offsets: IndexSet) {
        offsets.map { aircraft[$0] }.forEach(viewContext.delete)
        do {
            try viewContext.save()
        } catch {
            viewContext.rollback()
        }
    }
}

/// Small sheet for creating or editing one aircraft profile.
struct AircraftFormView: View {
    @Environment(\.managedObjectContext) private var viewContext
    @Environment(\.dismiss) private var dismiss

    /// The profile being edited, or nil when adding.
    let aircraft: Aircraft?

    @State private var name: String
    @State private var registration: String
    @State private var aircraftType: String
    @State private var category: AircraftCategory?
    @State private var aircraftClass: AircraftClass?
    /// Message shown when saving fails.
    @State private var saveError: String?

    init(aircraft: Aircraft? = nil) {
        self.aircraft = aircraft
        _name = State(initialValue: aircraft?.name ?? "")
        _registration = State(initialValue: aircraft?.registration ?? "")
        _aircraftType = State(initialValue: aircraft?.aircraftType ?? "")
        _category = State(initialValue: aircraft?.category.flatMap(AircraftCategory.init(rawValue:)))
        _aircraftClass = State(initialValue: aircraft?.aircraftClass.flatMap(AircraftClass.init(rawValue:)))
    }

    /// Registration is the only required field.
    private var isValid: Bool {
        !registration.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Registration (e.g. N12345)", text: $registration)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                    TextField("Aircraft Type (e.g. C172)", text: $aircraftType)
                        .textInputAutocapitalization(.characters)
                    CategoryClassPickers(category: $category, aircraftClass: $aircraftClass)
                }
                Section {
                    TextField("Name (e.g. Club 172)", text: $name)
                } footer: {
                    Text("Optional. When left empty, the profile is shown by its registration.")
                }
            }
            .dismissKeyboardOnTap()
            .navigationTitle(aircraft == nil ? "New Aircraft" : "Edit Aircraft")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                // Color-coded so the actions read at a glance:
                // red discards, blue saves.
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                        .tint(.red)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .tint(.blue)
                        .disabled(!isValid)
                }
            }
            .alert("Could Not Save", isPresented: .constant(saveError != nil)) {
                Button("OK") { saveError = nil }
            } message: {
                Text(saveError ?? "")
            }
        }
        .presentationDetents([.medium])
    }

    /// Writes the profile (creating it if needed) and dismisses on success.
    private func save() {
        let target = aircraft ?? Aircraft(context: viewContext)
        if target.id == nil { target.id = UUID() }
        target.name = name.trimmingCharacters(in: .whitespaces)
        target.registration = registration.trimmingCharacters(in: .whitespaces).uppercased()
        target.aircraftType = aircraftType.trimmingCharacters(in: .whitespaces).uppercased()
        target.category = category?.rawValue
        target.aircraftClass = aircraftClass?.rawValue
        do {
            try viewContext.save()
            dismiss()
        } catch {
            viewContext.rollback()
            saveError = error.localizedDescription
        }
    }
}

#Preview {
    NavigationStack {
        SavedAircraftView()
    }
    .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
