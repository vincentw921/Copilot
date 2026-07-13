//
//  FlightEntry.swift
//  Copilot
//
//  A plain value type representing one logbook entry, matching the
//  information a pilot must log under 14 CFR 61.51 (Pilot logbooks).
//

import Foundation
import CoreData

/// A single flight (or simulator session) in the pilot's logbook.
///
/// This is a value-type mirror of the Core Data `Item` entity. Views and
/// business logic (totals, currency, CSV export) work with `FlightEntry`
/// so they stay independent of Core Data and easy to unit test.
struct FlightEntry: Identifiable, Equatable {
    var id = UUID()

    // MARK: General (61.51(b)(1))
    /// Date of the flight.
    var date: Date = Date()
    /// Aircraft make and model, e.g. "C172".
    var aircraftType: String = ""
    /// FAA category (Airplane, Rotorcraft, …), when specified.
    var category: AircraftCategory?
    /// FAA class within the category (Single-Engine Land, …), when specified.
    var aircraftClass: AircraftClass?
    /// Aircraft identification (tail number), e.g. "N12345".
    var aircraftRegistration: String = ""
    /// Point of departure (airport identifier), e.g. "KSNA".
    var departureAirport: String = ""
    /// Point of arrival (airport identifier), e.g. "KLAX".
    var arrivalAirport: String = ""

    // MARK: Flight times, in decimal hours (61.51(b)(2))
    /// Total duration of flight.
    var totalTime: Double = 0
    /// Pilot-in-command time.
    var picTime: Double = 0
    /// Second-in-command time.
    var sicTime: Double = 0
    /// Solo time.
    var soloTime: Double = 0
    /// Cross-country time.
    var xcTime: Double = 0
    /// Night time.
    var nightTime: Double = 0
    /// Night-vision-goggle operation time.
    var nvgTime: Double = 0

    // MARK: Instrument conditions (61.51(b)(3))
    /// Actual instrument time (in IMC).
    var actualInstrumentTime: Double = 0
    /// Simulated instrument time (under the hood).
    var simulatedInstrumentTime: Double = 0

    // MARK: Takeoffs and landings
    var dayTakeoffs: Int = 0
    var dayFullStopLandings: Int = 0
    /// Day landings that were not to a full stop (touch-and-goes).
    var dayNonFullStopLandings: Int = 0
    var nightTakeoffs: Int = 0
    /// Night landings to a full stop (required for night currency, 61.57(b)).
    var nightFullStopLandings: Int = 0
    /// Night landings that were not to a full stop (count for day currency only).
    var nightNonFullStopLandings: Int = 0

    // MARK: Training
    /// Flight instruction given (as an instructor).
    var dualGivenTime: Double = 0
    /// Flight instruction received.
    var dualReceivedTime: Double = 0
    /// Time in a full flight simulator or flight training device.
    var simulatorTime: Double = 0
    /// Ground instruction given (as an instructor).
    var groundTrainingGivenTime: Double = 0
    /// Ground instruction received.
    var groundTrainingReceivedTime: Double = 0

    /// Free-form remarks, endorsements, maneuvers performed, etc.
    var notes: String = ""
}

// MARK: - Derived values

extension FlightEntry {
    /// "KSNA → KLAX", or a single airport when the entry only has one.
    var routeDescription: String {
        let airports = [departureAirport, arrivalAirport].filter { !$0.isEmpty }
        return airports.joined(separator: " → ")
    }

    /// True when the entry has everything 61.51 requires before it can be saved.
    var isValid: Bool {
        !aircraftType.trimmingCharacters(in: .whitespaces).isEmpty
            && !aircraftRegistration.trimmingCharacters(in: .whitespaces).isEmpty
            && !departureAirport.trimmingCharacters(in: .whitespaces).isEmpty
            && !arrivalAirport.trimmingCharacters(in: .whitespaces).isEmpty
            && totalTime > 0
    }
}

// MARK: - Core Data mapping

extension FlightEntry {
    /// Creates a value copy of a persisted `Item`.
    init(item: Item) {
        id = item.id ?? UUID()
        date = item.date ?? Date()
        aircraftType = item.aircraftType ?? ""
        category = item.category.flatMap(AircraftCategory.init(rawValue:))
        aircraftClass = item.aircraftClass.flatMap(AircraftClass.init(rawValue:))
        aircraftRegistration = item.aircraftRegistration ?? ""
        departureAirport = item.departureAirport ?? ""
        arrivalAirport = item.arrivalAirport ?? ""
        totalTime = item.totalTime
        picTime = item.picTime
        sicTime = item.sicTime
        soloTime = item.soloTime
        xcTime = item.xcTime
        nightTime = item.nightTime
        nvgTime = item.nvgTime
        actualInstrumentTime = item.actualInstrumentTime
        simulatedInstrumentTime = item.simulatedInstrumentTime
        dayTakeoffs = Int(item.dayTakeoffs)
        dayFullStopLandings = Int(item.dayFullStopLandings)
        dayNonFullStopLandings = Int(item.dayNonFullStopLandings)
        nightTakeoffs = Int(item.nightTakeoffs)
        nightFullStopLandings = Int(item.nightFullStopLandings)
        nightNonFullStopLandings = Int(item.nightNonFullStopLandings)
        dualGivenTime = item.dualGivenTime
        dualReceivedTime = item.dualReceivedTime
        simulatorTime = item.simulatorTime
        groundTrainingGivenTime = item.groundTrainingGivenTime
        groundTrainingReceivedTime = item.groundTrainingReceivedTime
        notes = item.notes ?? ""
    }

    /// Writes this entry's values into a managed `Item` (new or existing).
    /// The caller is responsible for saving the context.
    func apply(to item: Item) {
        item.id = id
        item.date = date
        item.aircraftType = aircraftType
        item.category = category?.rawValue
        item.aircraftClass = aircraftClass?.rawValue
        item.aircraftRegistration = aircraftRegistration.uppercased()
        item.departureAirport = departureAirport.uppercased()
        item.arrivalAirport = arrivalAirport.uppercased()
        item.totalTime = totalTime
        item.picTime = picTime
        item.sicTime = sicTime
        item.soloTime = soloTime
        item.xcTime = xcTime
        item.nightTime = nightTime
        item.nvgTime = nvgTime
        item.actualInstrumentTime = actualInstrumentTime
        item.simulatedInstrumentTime = simulatedInstrumentTime
        item.dayTakeoffs = Int16(clamping: dayTakeoffs)
        item.dayFullStopLandings = Int16(clamping: dayFullStopLandings)
        item.dayNonFullStopLandings = Int16(clamping: dayNonFullStopLandings)
        item.nightTakeoffs = Int16(clamping: nightTakeoffs)
        item.nightFullStopLandings = Int16(clamping: nightFullStopLandings)
        item.nightNonFullStopLandings = Int16(clamping: nightNonFullStopLandings)
        item.dualGivenTime = dualGivenTime
        item.dualReceivedTime = dualReceivedTime
        item.simulatorTime = simulatorTime
        item.groundTrainingGivenTime = groundTrainingGivenTime
        item.groundTrainingReceivedTime = groundTrainingReceivedTime
        item.notes = notes
    }
}
