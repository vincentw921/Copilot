package com.vincentwang.copilot.models

// FAA aircraft category and class hierarchy (14 CFR 61.5), ported from
// AircraftCategory.swift. A class always belongs to a category; Powered
// Lift and Glider have no class ratings at all.

/** Aircraft category, the top level of the FAA rating hierarchy. */
enum class AircraftCategory(val displayName: String) {
    AIRPLANE("Airplane"),
    ROTORCRAFT("Rotorcraft"),
    LIGHTER_THAN_AIR("Lighter-Than-Air"),
    POWERED_LIFT("Powered Lift"),
    GLIDER("Glider");

    /** The classes available within this category (empty for categories
     *  that have no class ratings). */
    val classes: List<AircraftClass>
        get() = when (this) {
            AIRPLANE -> listOf(
                AircraftClass.SINGLE_ENGINE_LAND, AircraftClass.MULTI_ENGINE_LAND,
                AircraftClass.SINGLE_ENGINE_SEA, AircraftClass.MULTI_ENGINE_SEA
            )
            ROTORCRAFT -> listOf(AircraftClass.HELICOPTER, AircraftClass.GYROPLANE)
            LIGHTER_THAN_AIR -> listOf(AircraftClass.AIRSHIP, AircraftClass.BALLOON)
            POWERED_LIFT, GLIDER -> emptyList()
        }

    companion object {
        /** Resolves the persisted display name ("Airplane") back to a value. */
        fun fromDisplayName(name: String?): AircraftCategory? =
            entries.firstOrNull { it.displayName == name }
    }
}

/** Aircraft class, the second level of the hierarchy. */
enum class AircraftClass(val displayName: String) {
    SINGLE_ENGINE_LAND("Single-Engine Land"),
    MULTI_ENGINE_LAND("Multi-Engine Land"),
    SINGLE_ENGINE_SEA("Single-Engine Sea"),
    MULTI_ENGINE_SEA("Multi-Engine Sea"),
    HELICOPTER("Helicopter"),
    GYROPLANE("Gyroplane"),
    AIRSHIP("Airship"),
    BALLOON("Balloon");

    companion object {
        fun fromDisplayName(name: String?): AircraftClass? =
            entries.firstOrNull { it.displayName == name }
    }
}
