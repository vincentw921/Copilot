//
//  CopilotTests.swift
//  CopilotTests
//
//  Unit tests for the pure logbook logic: totals, 61.57 currency,
//  and CSV export.
//

import Foundation
import Testing
@testable import Copilot

/// Builds a minimal valid entry, overridable per test.
private func makeEntry(
    daysAgo: Int = 0,
    totalTime: Double = 1.0,
    dayTakeoffs: Int = 0,
    dayFullStopLandings: Int = 0,
    nightTakeoffs: Int = 0,
    nightFullStopLandings: Int = 0,
    approachCount: Int = 0,
    holdCount: Int = 0,
    notes: String = ""
) -> FlightEntry {
    var entry = FlightEntry()
    entry.date = Calendar.current.date(byAdding: .day, value: -daysAgo, to: .now)!
    entry.aircraftType = "C172"
    entry.aircraftRegistration = "N12345"
    entry.departureAirport = "KSNA"
    entry.arrivalAirport = "KLAX"
    entry.totalTime = totalTime
    entry.dayTakeoffs = dayTakeoffs
    entry.dayFullStopLandings = dayFullStopLandings
    entry.nightTakeoffs = nightTakeoffs
    entry.nightFullStopLandings = nightFullStopLandings
    entry.approachCount = approachCount
    entry.holdCount = holdCount
    entry.notes = notes
    return entry
}

struct LogbookStatsTests {

    @Test func totalsSumAcrossEntries() {
        let entries = [
            makeEntry(totalTime: 1.5, dayFullStopLandings: 2),
            makeEntry(totalTime: 2.0, dayFullStopLandings: 1),
        ]
        let totals = LogbookStats.totals(for: entries)
        #expect(totals.totalTime == 3.5)
        #expect(totals.dayFullStopLandings == 3)
        #expect(totals.flightCount == 2)
    }

    @Test func dayCurrencyCountsBothDayAndNightOperations() {
        // 2 day + 1 night takeoff/landing inside 90 days → current for day.
        let entries = [
            makeEntry(daysAgo: 10, dayTakeoffs: 2, dayFullStopLandings: 2),
            makeEntry(daysAgo: 20, nightTakeoffs: 1, nightFullStopLandings: 1),
        ]
        let status = LogbookStats.dayCurrency(for: entries)
        #expect(status.takeoffs == 3)
        #expect(status.landings == 3)
        #expect(status.isCurrent)
    }

    @Test func dayCurrencyCountsTouchAndGoes() {
        // 61.57(a) doesn't require full stops, so touch-and-goes count.
        var entry = makeEntry(daysAgo: 5, dayTakeoffs: 3)
        entry.dayNonFullStopLandings = 3
        let status = LogbookStats.dayCurrency(for: [entry])
        #expect(status.landings == 3)
        #expect(status.isCurrent)
    }

    @Test func nightCurrencyIgnoresDayAndNonFullStopLandings() {
        var nightTouchAndGoes = makeEntry(daysAgo: 20, nightTakeoffs: 2, nightFullStopLandings: 2)
        nightTouchAndGoes.nightNonFullStopLandings = 5
        let entries = [
            makeEntry(daysAgo: 10, dayTakeoffs: 3, dayFullStopLandings: 3),
            nightTouchAndGoes,
        ]
        let status = LogbookStats.nightCurrency(for: entries)
        #expect(status.takeoffs == 2)
        #expect(status.landings == 2)
        #expect(!status.isCurrent)
    }

    @Test func currencyExcludesFlightsOlderThan90Days() {
        let entries = [
            makeEntry(daysAgo: 91, dayTakeoffs: 3, dayFullStopLandings: 3),
        ]
        let status = LogbookStats.dayCurrency(for: entries)
        #expect(status.takeoffs == 0)
        #expect(!status.isCurrent)
    }

    @Test func instrumentCurrencyRequiresApproachesAndHolds() {
        let entries = [
            makeEntry(daysAgo: 30, approachCount: 4, holdCount: 1),
            makeEntry(daysAgo: 60, approachCount: 2),
        ]
        let status = LogbookStats.instrumentCurrency(for: entries)
        #expect(status.approaches == 6)
        #expect(status.holds == 1)
        #expect(status.isCurrent)
    }

    @Test func instrumentCurrencyFailsWithoutHolds() {
        // 6 approaches but no holding procedures logged.
        let entries = [makeEntry(daysAgo: 10, approachCount: 6)]
        let status = LogbookStats.instrumentCurrency(for: entries)
        #expect(status.approaches == 6)
        #expect(status.holds == 0)
        #expect(!status.isCurrent)
    }

    @Test func instrumentCurrencyExcludesFlightsOlderThan6Months() {
        let entries = [
            makeEntry(daysAgo: 200, approachCount: 6, holdCount: 2),
        ]
        let status = LogbookStats.instrumentCurrency(for: entries)
        #expect(status.approaches == 0)
        #expect(status.holds == 0)
        #expect(!status.isCurrent)
    }

    @Test func categoryTotalsGroupAndSkipUncategorized() {
        var airplaneSEL = makeEntry(totalTime: 2.0)
        airplaneSEL.category = .airplane
        airplaneSEL.aircraftClass = .singleEngineLand
        var airplaneSEL2 = makeEntry(totalTime: 1.0)
        airplaneSEL2.category = .airplane
        airplaneSEL2.aircraftClass = .singleEngineLand
        var glider = makeEntry(totalTime: 0.5)
        glider.category = .glider
        let uncategorized = makeEntry(totalTime: 3.0)

        let totals = LogbookStats.categoryTotals(
            for: [airplaneSEL, airplaneSEL2, glider, uncategorized]
        )

        // Uncategorized time never produces a row; same pairs merge.
        #expect(totals.count == 2)
        #expect(totals[0].label == "Airplane · Single-Engine Land")
        #expect(totals[0].hours == 3.0)
        #expect(totals[1].label == "Glider")
        #expect(totals[1].hours == 0.5)
    }

    @Test func classHierarchyFollowsCategory() {
        #expect(AircraftCategory.airplane.classes.contains(.singleEngineLand))
        #expect(AircraftCategory.rotorcraft.classes == [.helicopter, .gyroplane])
        #expect(AircraftCategory.poweredLift.classes.isEmpty)
        #expect(AircraftCategory.glider.classes.isEmpty)
        #expect(!AircraftCategory.airplane.classes.contains(.balloon))
    }
}

struct CSVExporterTests {

    @Test func csvHasHeaderAndOneRowPerEntry() {
        let csv = CSVExporter.csv(for: [makeEntry(), makeEntry(daysAgo: 1)])
        let lines = csv.split(separator: "\n")
        #expect(lines.count == 3) // header + 2 rows
        #expect(lines[0].hasPrefix("Date,Aircraft Type,Category,Class,Registration"))
    }

    @Test func csvSortsOldestFirst() {
        let newest = makeEntry(daysAgo: 0)
        let oldest = makeEntry(daysAgo: 30)
        let csv = CSVExporter.csv(for: [newest, oldest])

        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let lines = csv.split(separator: "\n")
        #expect(lines[1].hasPrefix(formatter.string(from: oldest.date)))
        #expect(lines[2].hasPrefix(formatter.string(from: newest.date)))
    }

    @Test func csvEscapesCommasAndQuotes() {
        let entry = makeEntry(notes: "Steep turns, \"slow flight\"")
        let csv = CSVExporter.csv(for: [entry])
        #expect(csv.contains("\"Steep turns, \"\"slow flight\"\"\""))
    }

    @Test func csvIncludesCrossCountryColumn() {
        var entry = makeEntry(totalTime: 2.0)
        entry.xcTime = 2.0
        let csv = CSVExporter.csv(for: [entry])
        #expect(csv.contains("Cross Country"))
        #expect(csv.split(separator: "\n")[1].contains("2.0"))
    }

    @Test func csvIncludesApproachAndHoldColumns() {
        let entry = makeEntry(approachCount: 3, holdCount: 1)
        let csv = CSVExporter.csv(for: [entry])
        #expect(csv.contains("Approaches,Holds"))
        #expect(csv.split(separator: "\n")[1].contains("0.0,3,1,0"))
    }

    @Test func entryValidationRequiresCoreFields() {
        #expect(makeEntry().isValid)

        var missingAirport = makeEntry()
        missingAirport.arrivalAirport = " "
        #expect(!missingAirport.isValid)

        var zeroTime = makeEntry()
        zeroTime.totalTime = 0
        #expect(!zeroTime.isValid)
    }
}

struct AirportDatabaseTests {

    @Test func haversineMatchesKnownDistance() {
        // KSNA → KLAX is roughly 31 NM.
        let ksna = AirportDatabase.Coordinate(latitude: 33.6751, longitude: -117.8693)
        let klax = AirportDatabase.Coordinate(latitude: 33.9425, longitude: -118.4080)
        let distance = AirportDatabase.haversineNM(from: ksna, to: klax)
        #expect(distance > 25 && distance < 40)
    }

    @Test func lookupResolvesIcaoAndIataCodes() {
        // Same airport under both identifiers, case-insensitive.
        #expect(AirportDatabase.shared.coordinate(for: "KSNA") != nil)
        #expect(AirportDatabase.shared.coordinate(for: "sna") != nil)
        #expect(AirportDatabase.shared.coordinate(for: "ZZZZZZ") == nil)
    }

    @Test func routeDistanceClassifiesCrossCountry() throws {
        // KSNA → KSFO (~320 NM) is cross-country; KSNA → KLAX (~31 NM) is not.
        let long = try #require(AirportDatabase.shared.distanceNM(from: "KSNA", to: "KSFO"))
        let short = try #require(AirportDatabase.shared.distanceNM(from: "KSNA", to: "KLAX"))
        #expect(long >= AirportDatabase.crossCountryThresholdNM)
        #expect(short < AirportDatabase.crossCountryThresholdNM)
    }
}

struct AircraftProfileTests {

    @Test @MainActor func displayNameDefaultsToRegistration() {
        let context = PersistenceController(inMemory: true).container.viewContext
        let plane = Aircraft(context: context)
        plane.registration = "N12345"

        // No custom name → registration is the display name.
        #expect(plane.displayName == "N12345")
        plane.name = ""
        #expect(plane.displayName == "N12345")

        // A custom name wins once set.
        plane.name = "Club 172"
        #expect(plane.displayName == "Club 172")
    }
}

struct MonthGridTests {

    /// Fixed US-style calendar (weeks start Sunday) so tests are
    /// independent of the machine's locale.
    private var calendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.firstWeekday = 1
        cal.locale = Locale(identifier: "en_US")
        return cal
    }

    @Test func gridDaysPadsToTheFirstWeekday() throws {
        // July 1, 2026 is a Wednesday → 3 blanks (Sun, Mon, Tue), 31 days.
        let july = try #require(calendar.date(from: DateComponents(year: 2026, month: 7, day: 15)))
        let cells = MonthGrid.gridDays(for: july, calendar: calendar)

        #expect(cells.count == 3 + 31)
        #expect(cells.prefix(3).allSatisfy { $0 == nil })
        let firstDay = try #require(cells[3])
        #expect(calendar.component(.day, from: firstDay) == 1)
        let lastDay = try #require(cells.last ?? nil)
        #expect(calendar.component(.day, from: lastDay) == 31)
    }

    @Test func monthOffsetMovesToAdjacentMonths() throws {
        let july = try #require(calendar.date(from: DateComponents(year: 2026, month: 7, day: 15)))
        let next = MonthGrid.month(july, offsetBy: 1, calendar: calendar)
        let previous = MonthGrid.month(july, offsetBy: -1, calendar: calendar)
        #expect(calendar.component(.month, from: next) == 8)
        #expect(calendar.component(.month, from: previous) == 6)
        #expect(calendar.component(.day, from: next) == 1)
    }

    @Test func weekdaySymbolsStartAtFirstWeekday() {
        var monday = calendar
        monday.firstWeekday = 2
        #expect(MonthGrid.orderedWeekdaySymbols(for: monday).first == "M")
        #expect(MonthGrid.orderedWeekdaySymbols(for: calendar).first == "S")
    }
}
