//
//  FlightEntryFormView.swift
//  Copilot
//
//  Form for creating or editing a single logbook entry. Required 61.51
//  fields are always visible; less common columns are tucked into
//  collapsible groups so the form stays quick for a typical VFR flight.
//
//  Times are checkbox-driven: the pilot enters the total time once and
//  checks PIC/SIC, Solo, Night, etc. — the matching time columns fill in
//  automatically and can be fine-tuned under "Adjust Times".
//

import SwiftUI
import CoreData

/// Add/edit form presented as a sheet from the logbook list.
///
/// Pass `item: nil` to create a new entry, or an existing `Item` to edit it.
/// The form edits a local `FlightEntry` copy and only writes to Core Data
/// when the user taps Save.
struct FlightEntryFormView: View {
    @Environment(\.managedObjectContext) private var viewContext
    @Environment(\.dismiss) private var dismiss

    /// Who acted as required crew, driving automatic PIC/SIC time.
    /// "None" covers flights logging neither (e.g. dual received only).
    enum Role: String, CaseIterable, Identifiable {
        case pic = "PIC"
        case sic = "SIC"
        case none = "None"
        var id: String { rawValue }
    }

    /// The persisted record being edited, or nil when adding a new one.
    let item: Item?

    /// Saved aircraft profiles offered in the aircraft picker.
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Aircraft.registration, ascending: true)]
    ) private var savedAircraft: FetchedResults<Aircraft>

    /// Mirrors the Settings toggle; hides the NVG checkbox when disabled.
    @AppStorage("nvgLoggingEnabled") private var nvgLoggingEnabled = false

    /// Working copy the form fields bind to.
    @State private var entry: FlightEntry

    /// Selected saved aircraft, or nil to enter the aircraft manually.
    @State private var selectedAircraftID: NSManagedObjectID?

    // Checkboxes that auto-populate their time columns from the total.
    @State private var role: Role
    @State private var isSolo: Bool
    @State private var isNight: Bool
    @State private var isNVG: Bool
    @State private var isDualGiven: Bool
    @State private var isDualReceived: Bool
    @State private var isSimulator: Bool
    /// Whether this flight is logged as cross-country. Auto-checked when
    /// the route is ≥ 50 NM (14 CFR 61.1); the pilot can override it.
    @State private var isCrossCountry: Bool

    /// Message shown when saving fails (e.g. the store is unavailable).
    @State private var saveError: String?

    /// Regenerated on every date change to collapse the compact date
    /// picker's popover as soon as a day is tapped.
    @State private var datePickerID = UUID()

    // Collapsible optional groups.
    @State private var showAdjustTimes = false
    @State private var showInstrument = false
    @State private var showTraining = false

    init(item: Item? = nil) {
        self.item = item
        let entry = item.map(FlightEntry.init) ?? FlightEntry()
        _entry = State(initialValue: entry)

        // Derive the checkboxes from the stored times when editing;
        // a brand-new flight defaults to PIC (the common case).
        let initialRole: Role = if entry.picTime > 0 { .pic }
            else if entry.sicTime > 0 { .sic }
            else if item == nil { .pic }
            else { .none }
        _role = State(initialValue: initialRole)
        _isSolo = State(initialValue: entry.soloTime > 0)
        _isNight = State(initialValue: entry.nightTime > 0)
        _isNVG = State(initialValue: entry.nvgTime > 0)
        _isDualGiven = State(initialValue: entry.dualGivenTime > 0)
        _isDualReceived = State(initialValue: entry.dualReceivedTime > 0)
        _isSimulator = State(initialValue: entry.simulatorTime > 0)
        _isCrossCountry = State(initialValue: entry.xcTime > 0)
    }

    /// Great-circle route distance, when both airports are in the database.
    private var routeDistanceNM: Double? {
        AirportDatabase.shared.distanceNM(
            from: entry.departureAirport,
            to: entry.arrivalAirport
        )
    }

    var body: some View {
        NavigationStack {
            observedForm
                .navigationTitle(item == nil ? "New Flight" : "Edit Flight")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Save") { save() }
                            .disabled(!entry.isValid)
                    }
                }
                .alert("Could Not Save", isPresented: .constant(saveError != nil)) {
                    Button("OK") { saveError = nil }
                } message: {
                    Text(saveError ?? "")
                }
        }
    }

    /// The form sections. Kept separate from the observer chains below so
    /// the compiler can type-check each layer independently.
    private var formSections: some View {
        Form {
            flightSection
            timeSection
            crossCountrySection
            takeoffsSection
            landingsSection
            instrumentSection
            trainingSection
            notesSection
        }
    }

    /// Field observers: date popover collapse, XC auto-check, profile fill.
    private var fieldObservedForm: some View {
        formSections
            .onChange(of: entry.date) { datePickerID = UUID() }
            .onChange(of: entry.departureAirport) { updateCrossCountryAutoCheck() }
            .onChange(of: entry.arrivalAirport) { updateCrossCountryAutoCheck() }
            .onChange(of: selectedAircraftID) { fillFromSelectedAircraft() }
    }

    /// Checkbox observers: each toggle fills or clears its time column.
    private var observedForm: some View {
        fieldObservedForm
            .onChange(of: entry.totalTime) { applyAutomaticTimes() }
            .onChange(of: role) {
                entry.picTime = role == .pic ? entry.totalTime : 0
                entry.sicTime = role == .sic ? entry.totalTime : 0
            }
            .onChange(of: isSolo) { entry.soloTime = isSolo ? entry.totalTime : 0 }
            .onChange(of: isNight) { entry.nightTime = isNight ? entry.totalTime : 0 }
            .onChange(of: isNVG) { entry.nvgTime = isNVG ? entry.totalTime : 0 }
            .onChange(of: isDualGiven) { entry.dualGivenTime = isDualGiven ? entry.totalTime : 0 }
            .onChange(of: isDualReceived) { entry.dualReceivedTime = isDualReceived ? entry.totalTime : 0 }
            .onChange(of: isSimulator) { entry.simulatorTime = isSimulator ? entry.totalTime : 0 }
            .onChange(of: isCrossCountry) { entry.xcTime = isCrossCountry ? entry.totalTime : 0 }
    }

    // MARK: Sections

    /// Date, aircraft (saved profile or manual), and route with
    /// unknown-airport warnings — all required by 61.51(b)(1).
    private var flightSection: some View {
        Section("Flight") {
            // Flights can't be logged for the future (61.51 records what
            // already happened), so the picker stops at today. The changing
            // id collapses the popover right after a day is tapped.
            DatePicker("Date", selection: $entry.date, in: ...Date.now, displayedComponents: .date)
                .id(datePickerID)

            // Either pick a saved profile or enter the aircraft manually.
            // The navigationLink style renders options as a list, which
            // (unlike a menu) honors the bold styling below.
            if !savedAircraft.isEmpty {
                Picker("Aircraft", selection: $selectedAircraftID) {
                    Text("Enter Manually")
                        .bold()
                        .foregroundStyle(.secondary)
                        .tag(nil as NSManagedObjectID?)
                    ForEach(savedAircraft) { plane in
                        Text(plane.displayName).tag(Optional(plane.objectID))
                    }
                }
                .pickerStyle(.navigationLink)
            }
            if selectedAircraftID == nil {
                TextField("Aircraft Type (e.g. C172)", text: $entry.aircraftType)
                    .textInputAutocapitalization(.characters)
                TextField("Registration (e.g. N12345)", text: $entry.aircraftRegistration)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                CategoryClassPickers(
                    category: $entry.category,
                    aircraftClass: $entry.aircraftClass
                )
            }

            TextField("From (e.g. KSNA)", text: $entry.departureAirport)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
            if isUnknownAirport(entry.departureAirport) {
                unknownAirportWarning(for: entry.departureAirport)
            }
            TextField("To (e.g. KLAX)", text: $entry.arrivalAirport)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
            if isUnknownAirport(entry.arrivalAirport) {
                unknownAirportWarning(for: entry.arrivalAirport)
            }
        }
    }

    /// Total time plus the checkboxes that fill the time columns
    /// automatically, with an advanced group for partial hours.
    private var timeSection: some View {
        Section {
            HoursField(label: "Total Time", value: $entry.totalTime)
            Picker("Role", selection: $role) {
                ForEach(Role.allCases) { role in
                    Text(role.rawValue).tag(role)
                }
            }
            .pickerStyle(.segmented)
            Toggle("Solo", isOn: $isSolo)
            Toggle("Night", isOn: $isNight)
            if nvgLoggingEnabled {
                Toggle("NVG", isOn: $isNVG)
            }
            DisclosureGroup("Adjust Times", isExpanded: $showAdjustTimes) {
                HoursField(label: "PIC", value: $entry.picTime)
                HoursField(label: "SIC", value: $entry.sicTime)
                HoursField(label: "Solo", value: $entry.soloTime)
                HoursField(label: "Night", value: $entry.nightTime)
                if nvgLoggingEnabled {
                    HoursField(label: "NVG", value: $entry.nvgTime)
                }
            }
        } header: {
            Text("Flight Time (hours)")
        } footer: {
            Text("Checked times fill in from the total automatically. Expand Adjust Times to log partial hours (e.g. 0.5 night of a 2.0 flight).")
        }
    }

    /// Route distance readout and the cross-country checkbox.
    private var crossCountrySection: some View {
        Section {
            if let distance = routeDistanceNM {
                HStack {
                    Text("Route Distance")
                    Spacer()
                    Text("\(distance, format: .number.precision(.fractionLength(0))) NM")
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
            }
            Toggle("Cross Country", isOn: $isCrossCountry)
            if isCrossCountry {
                HoursField(label: "Cross Country Time", value: $entry.xcTime)
            }
        } footer: {
            Text("Checked automatically when the route is 50 NM or more (14 CFR 61.1). You can override it.")
        }
    }

    /// Takeoff counts, needed for 61.57 currency tracking.
    private var takeoffsSection: some View {
        Section("Takeoffs") {
            CountField(label: "Day", value: $entry.dayTakeoffs)
            CountField(label: "Night", value: $entry.nightTakeoffs)
        }
    }

    /// Landing counts. Full-stop and non-full-stop (touch-and-go) are
    /// tracked separately because night currency only accepts full stops.
    private var landingsSection: some View {
        Section("Landings") {
            CountField(label: "Day (full stop)", value: $entry.dayFullStopLandings)
            CountField(label: "Day (non-full stop)", value: $entry.dayNonFullStopLandings)
            CountField(label: "Night (full stop)", value: $entry.nightFullStopLandings)
            CountField(label: "Night (non-full stop)", value: $entry.nightNonFullStopLandings)
        }
    }

    /// Actual/simulated instrument time — optional, 61.51(b)(3).
    private var instrumentSection: some View {
        Section {
            DisclosureGroup("Instrument", isExpanded: $showInstrument) {
                HoursField(label: "Actual Instrument", value: $entry.actualInstrumentTime)
                HoursField(label: "Simulated Instrument", value: $entry.simulatedInstrumentTime)
            }
        }
    }

    /// Instruction given/received and simulator time. The checkboxes fill
    /// each column with the total; exact values (and ground training) live
    /// under the disclosure.
    private var trainingSection: some View {
        Section {
            Toggle("Dual Given", isOn: $isDualGiven)
            Toggle("Dual Received", isOn: $isDualReceived)
            Toggle("Simulator / FTD", isOn: $isSimulator)
            DisclosureGroup("Adjust Training Times", isExpanded: $showTraining) {
                HoursField(label: "Dual Given", value: $entry.dualGivenTime)
                HoursField(label: "Dual Received", value: $entry.dualReceivedTime)
                HoursField(label: "Simulator / FTD", value: $entry.simulatorTime)
                HoursField(label: "Ground Training Given", value: $entry.groundTrainingGivenTime)
                HoursField(label: "Ground Training Received", value: $entry.groundTrainingReceivedTime)
            }
        } header: {
            Text("Training")
        } footer: {
            Text("Checked training times fill in from the total automatically.")
        }
    }

    /// Remarks, endorsements, and maneuvers.
    private var notesSection: some View {
        Section("Notes") {
            TextField("Remarks, endorsements…", text: $entry.notes, axis: .vertical)
                .lineLimit(3...6)
        }
    }

    // MARK: Airport validation

    /// True when `code` is non-empty but not in the airport database —
    /// likely a typo, or a private strip we don't know about.
    private func isUnknownAirport(_ code: String) -> Bool {
        let trimmed = code.trimmingCharacters(in: .whitespaces)
        return !trimmed.isEmpty && AirportDatabase.shared.coordinate(for: trimmed) == nil
    }

    /// The warning row shown under an unrecognized airport field.
    private func unknownAirportWarning(for code: String) -> some View {
        Label(
            "\(code.trimmingCharacters(in: .whitespaces).uppercased()) not found — check the identifier",
            systemImage: "exclamationmark.triangle"
        )
        .font(.footnote)
        .foregroundStyle(.orange)
    }

    // MARK: Automatic times

    /// Copies the aircraft fields out of the selected saved profile.
    /// Switching back to "Enter Manually" clears them for a fresh start.
    private func fillFromSelectedAircraft() {
        guard let id = selectedAircraftID,
              let plane = try? viewContext.existingObject(with: id) as? Aircraft else {
            entry.aircraftRegistration = ""
            entry.aircraftType = ""
            entry.category = nil
            entry.aircraftClass = nil
            return
        }
        entry.aircraftRegistration = plane.registration ?? ""
        entry.aircraftType = plane.aircraftType ?? ""
        entry.category = plane.category.flatMap(AircraftCategory.init(rawValue:))
        entry.aircraftClass = plane.aircraftClass.flatMap(AircraftClass.init(rawValue:))
    }

    /// Refills every checked time column when the total changes.
    private func applyAutomaticTimes() {
        let total = entry.totalTime
        if role == .pic { entry.picTime = total }
        if role == .sic { entry.sicTime = total }
        if isSolo { entry.soloTime = total }
        if isNight { entry.nightTime = total }
        if isNVG { entry.nvgTime = total }
        if isDualGiven { entry.dualGivenTime = total }
        if isDualReceived { entry.dualReceivedTime = total }
        if isSimulator { entry.simulatorTime = total }
        if isCrossCountry { entry.xcTime = total }
    }

    /// Re-evaluates the cross-country checkbox from the route distance.
    /// Runs whenever an airport field changes; only flips the toggle when
    /// both airports resolve, so a manual override survives typos.
    private func updateCrossCountryAutoCheck() {
        guard let distance = routeDistanceNM else { return }
        isCrossCountry = distance >= AirportDatabase.crossCountryThresholdNM
    }

    // MARK: Saving

    /// Writes the working copy into Core Data (creating the record if needed)
    /// and dismisses on success. CloudKit sync happens automatically.
    private func save() {
        // The checkboxes are authoritative: unchecked columns save as zero,
        // checked ones keep any fine-tuned value (defaulting to the total).
        if role != .pic { entry.picTime = 0 }
        if role != .sic { entry.sicTime = 0 }
        if !isSolo { entry.soloTime = 0 }
        if !isNight { entry.nightTime = 0 }
        if nvgLoggingEnabled && !isNVG { entry.nvgTime = 0 }
        if !isDualGiven { entry.dualGivenTime = 0 }
        if !isDualReceived { entry.dualReceivedTime = 0 }
        if !isSimulator { entry.simulatorTime = 0 }
        if !isCrossCountry {
            entry.xcTime = 0
        } else if entry.xcTime == 0 {
            entry.xcTime = entry.totalTime
        }

        let target = item ?? Item(context: viewContext)
        entry.apply(to: target)
        do {
            try viewContext.save()
            dismiss()
        } catch {
            viewContext.rollback()
            saveError = error.localizedDescription
        }
    }
}

// MARK: - Reusable rows

/// Labeled decimal-hours input, right-aligned like a paper logbook column.
/// A zero value renders as an empty field (grey "0.0" placeholder) so the
/// pilot can type straight away without clearing a default first.
struct HoursField: View {
    let label: String
    @Binding var value: Double

    /// Maps 0 ↔ empty so the text field never shows a literal "0".
    private var optionalValue: Binding<Double?> {
        Binding(
            get: { value == 0 ? nil : value },
            set: { value = $0 ?? 0 }
        )
    }

    var body: some View {
        HStack {
            Text(label)
            Spacer()
            TextField("0.0", value: optionalValue, format: .number.precision(.fractionLength(0...1)))
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.trailing)
                .frame(width: 80)
        }
    }
}

/// FAA category picker with a dependent class picker (14 CFR 61.5).
/// The class list follows the chosen category and disappears entirely
/// for categories without class ratings (Powered Lift, Glider).
struct CategoryClassPickers: View {
    @Binding var category: AircraftCategory?
    @Binding var aircraftClass: AircraftClass?

    var body: some View {
        Picker("Category", selection: $category) {
            Text("Not Specified")
                .italic()
                .foregroundStyle(.secondary)
                .tag(nil as AircraftCategory?)
            ForEach(AircraftCategory.allCases) { category in
                Text(category.rawValue).tag(Optional(category))
            }
        }
        .onChange(of: category) {
            // Drop a class that doesn't belong to the new category.
            if let aircraftClass, !(category?.classes.contains(aircraftClass) ?? false) {
                self.aircraftClass = nil
            }
        }

        if let category, !category.classes.isEmpty {
            Picker("Class", selection: $aircraftClass) {
                Text("Not Specified")
                    .italic()
                    .foregroundStyle(.secondary)
                    .tag(nil as AircraftClass?)
                ForEach(category.classes) { aircraftClass in
                    Text(aircraftClass.rawValue).tag(Optional(aircraftClass))
                }
            }
        }
    }
}

/// Labeled whole-number input with a stepper for takeoff/landing counts.
struct CountField: View {
    let label: String
    @Binding var value: Int

    var body: some View {
        Stepper(value: $value, in: 0...99) {
            HStack {
                Text(label)
                Spacer()
                Text("\(value)")
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            }
        }
    }
}

#Preview {
    FlightEntryFormView()
        .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
