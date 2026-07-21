//
//  LogbookStats.swift
//  Copilot
//
//  Pure aggregation over logbook entries: career totals and
//  FAR 61.57 recent-experience (currency) checks.
//

import Foundation

/// Career totals summed across a set of logbook entries.
struct LogbookTotals {
    var totalTime: Double = 0
    var picTime: Double = 0
    var sicTime: Double = 0
    var soloTime: Double = 0
    var xcTime: Double = 0
    var nightTime: Double = 0
    var nvgTime: Double = 0
    var actualInstrumentTime: Double = 0
    var simulatedInstrumentTime: Double = 0
    var dualGivenTime: Double = 0
    var dualReceivedTime: Double = 0
    var simulatorTime: Double = 0
    var groundTrainingGivenTime: Double = 0
    var groundTrainingReceivedTime: Double = 0
    var dayTakeoffs: Int = 0
    var dayFullStopLandings: Int = 0
    var dayNonFullStopLandings: Int = 0
    var nightTakeoffs: Int = 0
    var nightFullStopLandings: Int = 0
    var nightNonFullStopLandings: Int = 0

    /// Number of flights included in these totals.
    var flightCount: Int = 0
}

/// Result of a 14 CFR 61.57 passenger-carrying currency check.
struct CurrencyStatus {
    /// Takeoffs performed within the 90-day window.
    var takeoffs: Int
    /// Qualifying landings within the 90-day window (any landing for day
    /// currency; full-stop only for night).
    var landings: Int

    /// True when both counts meet the required 3 within the preceding 90 days.
    var isCurrent: Bool { takeoffs >= 3 && landings >= 3 }
}

/// Result of a 14 CFR 61.57(c) instrument currency check.
struct InstrumentCurrencyStatus {
    /// Instrument approaches logged within the preceding 6 calendar months.
    var approaches: Int
    /// Holding procedures logged within the same window.
    var holds: Int

    /// True when both parts of 61.57(c) are satisfied: 6 approaches, plus
    /// at least one holding procedure (and course tracking), within the
    /// preceding 6 months.
    var isCurrent: Bool { approaches >= 6 && holds >= 1 }
}

/// Total time flown in one category (and class, when logged).
struct CategoryClassTotal: Identifiable {
    let category: AircraftCategory
    let aircraftClass: AircraftClass?
    let hours: Double

    /// "Airplane · Single-Engine Land", or just the category when no
    /// class was logged.
    var label: String {
        if let aircraftClass {
            return "\(category.rawValue) · \(aircraftClass.rawValue)"
        }
        return category.rawValue
    }

    var id: String { label }
}

/// Stateless calculators over `[FlightEntry]`.
enum LogbookStats {
    /// Sums every logged column across `entries`.
    static func totals(for entries: [FlightEntry]) -> LogbookTotals {
        var t = LogbookTotals()
        for e in entries {
            t.totalTime += e.totalTime
            t.picTime += e.picTime
            t.sicTime += e.sicTime
            t.soloTime += e.soloTime
            t.xcTime += e.xcTime
            t.nightTime += e.nightTime
            t.nvgTime += e.nvgTime
            t.actualInstrumentTime += e.actualInstrumentTime
            t.simulatedInstrumentTime += e.simulatedInstrumentTime
            t.dualGivenTime += e.dualGivenTime
            t.dualReceivedTime += e.dualReceivedTime
            t.simulatorTime += e.simulatorTime
            t.groundTrainingGivenTime += e.groundTrainingGivenTime
            t.groundTrainingReceivedTime += e.groundTrainingReceivedTime
            t.dayTakeoffs += e.dayTakeoffs
            t.dayFullStopLandings += e.dayFullStopLandings
            t.dayNonFullStopLandings += e.dayNonFullStopLandings
            t.nightTakeoffs += e.nightTakeoffs
            t.nightFullStopLandings += e.nightFullStopLandings
            t.nightNonFullStopLandings += e.nightNonFullStopLandings
            t.flightCount += 1
        }
        return t
    }

    /// Total time per category/class pair, most-flown first. Entries with
    /// no category (or no time) are skipped, so zero rows never appear.
    static func categoryTotals(for entries: [FlightEntry]) -> [CategoryClassTotal] {
        var hours: [String: (AircraftCategory, AircraftClass?, Double)] = [:]
        for e in entries {
            guard let category = e.category, e.totalTime > 0 else { continue }
            let key = "\(category.rawValue)|\(e.aircraftClass?.rawValue ?? "")"
            hours[key, default: (category, e.aircraftClass, 0)].2 += e.totalTime
        }
        return hours.values
            .map { CategoryClassTotal(category: $0.0, aircraftClass: $0.1, hours: $0.2) }
            .sorted { $0.hours > $1.hours }
    }

    /// Day passenger-carrying currency per 61.57(a): 3 takeoffs and
    /// 3 landings within the preceding 90 days. Day and night operations
    /// both count, and the landings need not be to a full stop.
    static func dayCurrency(for entries: [FlightEntry], asOf now: Date = Date()) -> CurrencyStatus {
        let recent = entries.filter { $0.date >= windowStart(from: now) }
        return CurrencyStatus(
            takeoffs: recent.reduce(0) { $0 + $1.dayTakeoffs + $1.nightTakeoffs },
            landings: recent.reduce(0) {
                $0 + $1.dayFullStopLandings + $1.dayNonFullStopLandings
                    + $1.nightFullStopLandings + $1.nightNonFullStopLandings
            }
        )
    }

    /// Night passenger-carrying currency per 61.57(b): 3 takeoffs and
    /// 3 full-stop landings at night within the preceding 90 days.
    static func nightCurrency(for entries: [FlightEntry], asOf now: Date = Date()) -> CurrencyStatus {
        let recent = entries.filter { $0.date >= windowStart(from: now) }
        return CurrencyStatus(
            takeoffs: recent.reduce(0) { $0 + $1.nightTakeoffs },
            landings: recent.reduce(0) { $0 + $1.nightFullStopLandings }
        )
    }

    /// Instrument currency per 61.57(c): 6 instrument approaches, holding
    /// procedures, and intercepting/tracking courses, all within the
    /// preceding 6 calendar months.
    static func instrumentCurrency(for entries: [FlightEntry], asOf now: Date = Date()) -> InstrumentCurrencyStatus {
        let start = instrumentWindowStart(from: now)
        let recent = entries.filter { $0.date >= start }
        return InstrumentCurrencyStatus(
            approaches: recent.reduce(0) { $0 + $1.approachCount },
            holds: recent.reduce(0) { $0 + $1.holdCount }
        )
    }

    /// The start of the preceding-90-day window used by 61.57(a)/(b).
    private static func windowStart(from now: Date) -> Date {
        Calendar.current.date(byAdding: .day, value: -90, to: now) ?? now
    }

    /// The start of the preceding-6-month window used by 61.57(c).
    private static func instrumentWindowStart(from now: Date) -> Date {
        Calendar.current.date(byAdding: .month, value: -6, to: now) ?? now
    }
}
