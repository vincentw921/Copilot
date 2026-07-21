//
//  CSVExporter.swift
//  Copilot
//
//  Turns logbook entries into a spreadsheet-ready CSV file that can be
//  shared, saved, or printed from the iOS share sheet.
//

import Foundation

/// Builds CSV text from logbook entries.
enum CSVExporter {
    /// Column headers, in the order columns appear in the export.
    static let headers = [
        "Date", "Aircraft Type", "Category", "Class", "Registration", "From", "To",
        "Total Time", "PIC", "SIC", "Solo", "Cross Country", "Night", "NVG",
        "Actual Instrument", "Simulated Instrument", "Approaches", "Holds",
        "Day Takeoffs", "Day Full-Stop Landings", "Day Non-Full-Stop Landings",
        "Night Takeoffs", "Night Full-Stop Landings", "Night Non-Full-Stop Landings",
        "Dual Given", "Dual Received", "Simulator",
        "Ground Training Given", "Ground Training Received",
        "Notes",
    ]

    /// Renders `entries` as CSV, one row per flight, oldest first
    /// (the conventional order of a paper logbook).
    static func csv(for entries: [FlightEntry]) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"

        var rows = [headers.map(escape).joined(separator: ",")]
        for e in entries.sorted(by: { $0.date < $1.date }) {
            let fields: [String] = [
                dateFormatter.string(from: e.date),
                e.aircraftType,
                e.category?.rawValue ?? "", e.aircraftClass?.rawValue ?? "",
                e.aircraftRegistration,
                e.departureAirport, e.arrivalAirport,
                hours(e.totalTime), hours(e.picTime), hours(e.sicTime),
                hours(e.soloTime), hours(e.xcTime), hours(e.nightTime), hours(e.nvgTime),
                hours(e.actualInstrumentTime), hours(e.simulatedInstrumentTime),
                String(e.approachCount), String(e.holdCount),
                String(e.dayTakeoffs), String(e.dayFullStopLandings), String(e.dayNonFullStopLandings),
                String(e.nightTakeoffs), String(e.nightFullStopLandings), String(e.nightNonFullStopLandings),
                hours(e.dualGivenTime), hours(e.dualReceivedTime), hours(e.simulatorTime),
                hours(e.groundTrainingGivenTime), hours(e.groundTrainingReceivedTime),
                e.notes,
            ]
            rows.append(fields.map(escape).joined(separator: ","))
        }
        return rows.joined(separator: "\n") + "\n"
    }

    /// Formats decimal hours with one fractional digit ("1.5", "0.0").
    private static func hours(_ value: Double) -> String {
        String(format: "%.1f", value)
    }

    /// Quotes a field when it contains a comma, quote, or newline (RFC 4180).
    private static func escape(_ field: String) -> String {
        guard field.contains(where: { ",\"\n\r".contains($0) }) else { return field }
        return "\"" + field.replacingOccurrences(of: "\"", with: "\"\"") + "\""
    }
}

extension CSVExporter {
    /// Writes `entries` as a CSV file in the temporary directory and
    /// returns its URL, e.g. …/Logbook-2026-07-13.csv.
    ///
    /// Sharing a concrete file URL (rather than a `Transferable`
    /// `FileRepresentation`) is what lets the share sheet's "Save to
    /// Files" work — the file must already exist when the extension
    /// receives it. The file is only rewritten when the contents changed,
    /// so calling this from a view body is cheap.
    static func exportFile(for entries: [FlightEntry], date: Date = Date()) -> URL {
        let stamp = DateFormatter()
        stamp.dateFormat = "yyyy-MM-dd"
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("Logbook-\(stamp.string(from: date)).csv")

        let contents = csv(for: entries)
        if (try? String(contentsOf: url, encoding: .utf8)) != contents {
            try? contents.write(to: url, atomically: true, encoding: .utf8)
        }
        return url
    }
}
