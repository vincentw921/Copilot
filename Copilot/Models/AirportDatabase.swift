//
//  AirportDatabase.swift
//  Copilot
//
//  Offline airport-coordinate lookup and great-circle distance, used to
//  detect cross-country flights (14 CFR 61.1: > 50 NM from the point of
//  departure for most certificate requirements).
//
//  Data source: the bundled airports.csv, condensed from the public-domain
//  OurAirports dataset (ourairports.com). Each row is "code,lat,lon" and
//  the same airport appears under every identifier a pilot might type
//  (ICAO "KSNA", IATA/local "SNA", etc.).
//

import Foundation

/// Coordinate lookup over the bundled airport database.
final class AirportDatabase {
    /// Shared instance; the CSV is parsed once on first lookup.
    static let shared = AirportDatabase()

    /// A geographic coordinate in degrees.
    struct Coordinate {
        let latitude: Double
        let longitude: Double
    }

    /// Identifier → coordinate, loaded lazily from the bundle.
    private lazy var airports: [String: Coordinate] = Self.loadAirports()

    /// The distance in nautical miles at which a flight counts as
    /// cross-country for most 61.1(b) purposes.
    static let crossCountryThresholdNM = 50.0

    /// Looks up an airport by identifier (case-insensitive), e.g. "KSNA" or "SNA".
    func coordinate(for identifier: String) -> Coordinate? {
        airports[identifier.trimmingCharacters(in: .whitespaces).uppercased()]
    }

    /// Great-circle distance in nautical miles between two airports,
    /// or nil when either identifier is unknown.
    func distanceNM(from departure: String, to arrival: String) -> Double? {
        guard let from = coordinate(for: departure),
              let to = coordinate(for: arrival) else { return nil }
        return Self.haversineNM(from: from, to: to)
    }

    /// Haversine great-circle distance in nautical miles.
    static func haversineNM(from: Coordinate, to: Coordinate) -> Double {
        let earthRadiusNM = 3440.065
        let lat1 = from.latitude * .pi / 180
        let lat2 = to.latitude * .pi / 180
        let dLat = lat2 - lat1
        let dLon = (to.longitude - from.longitude) * .pi / 180

        let a = sin(dLat / 2) * sin(dLat / 2)
            + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return earthRadiusNM * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /// Parses the bundled "code,lat,lon" CSV into the lookup table.
    private static func loadAirports() -> [String: Coordinate] {
        guard let url = Bundle.main.url(forResource: "airports", withExtension: "csv"),
              let contents = try? String(contentsOf: url, encoding: .utf8) else {
            assertionFailure("airports.csv missing from bundle")
            return [:]
        }

        var result: [String: Coordinate] = [:]
        result.reserveCapacity(50_000)
        for line in contents.split(separator: "\n").dropFirst() { // skip header
            let fields = line.split(separator: ",")
            guard fields.count == 3,
                  let lat = Double(fields[1]),
                  let lon = Double(fields[2]) else { continue }
            result[String(fields[0])] = Coordinate(latitude: lat, longitude: lon)
        }
        return result
    }
}
