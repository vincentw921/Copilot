//
//  AircraftCategory.swift
//  Copilot
//
//  FAA aircraft category and class hierarchy (14 CFR 61.5). A class
//  always belongs to a category; Powered Lift and Glider have no
//  class ratings at all.
//

import Foundation

/// Aircraft category, the top level of the FAA rating hierarchy.
enum AircraftCategory: String, CaseIterable, Identifiable {
    case airplane = "Airplane"
    case rotorcraft = "Rotorcraft"
    case lighterThanAir = "Lighter-Than-Air"
    case poweredLift = "Powered Lift"
    case glider = "Glider"

    var id: String { rawValue }

    /// The classes available within this category (empty for categories
    /// that have no class ratings).
    var classes: [AircraftClass] {
        switch self {
        case .airplane:
            return [.singleEngineLand, .multiEngineLand, .singleEngineSea, .multiEngineSea]
        case .rotorcraft:
            return [.helicopter, .gyroplane]
        case .lighterThanAir:
            return [.airship, .balloon]
        case .poweredLift, .glider:
            return []
        }
    }
}

/// Aircraft class, the second level of the hierarchy.
enum AircraftClass: String, CaseIterable, Identifiable {
    case singleEngineLand = "Single-Engine Land"
    case multiEngineLand = "Multi-Engine Land"
    case singleEngineSea = "Single-Engine Sea"
    case multiEngineSea = "Multi-Engine Sea"
    case helicopter = "Helicopter"
    case gyroplane = "Gyroplane"
    case airship = "Airship"
    case balloon = "Balloon"

    var id: String { rawValue }
}
