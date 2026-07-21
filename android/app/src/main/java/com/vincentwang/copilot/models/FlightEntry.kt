package com.vincentwang.copilot.models

import com.vincentwang.copilot.data.Item
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * A single flight (or simulator session) in the pilot's logbook, matching
 * the information a pilot must log under 14 CFR 61.51. Ported from
 * FlightEntry.swift.
 *
 * This is a value-type mirror of the Room `Item` entity. Screens and
 * business logic (totals, currency, CSV export) work with `FlightEntry`
 * so they stay independent of Room and easy to unit test.
 */
data class FlightEntry(
    val id: String = UUID.randomUUID().toString(),

    // General (61.51(b)(1))
    val date: LocalDate = LocalDate.now(),
    /** Aircraft make and model, e.g. "C172". */
    val aircraftType: String = "",
    /** FAA category (Airplane, Rotorcraft, …), when specified. */
    val category: AircraftCategory? = null,
    /** FAA class within the category (Single-Engine Land, …), when specified. */
    val aircraftClass: AircraftClass? = null,
    /** Aircraft identification (tail number), e.g. "N12345". */
    val aircraftRegistration: String = "",
    /** Point of departure (airport identifier), e.g. "KSNA". */
    val departureAirport: String = "",
    /** Point of arrival (airport identifier), e.g. "KLAX". */
    val arrivalAirport: String = "",

    // Flight times, in decimal hours (61.51(b)(2))
    val totalTime: Double = 0.0,
    val picTime: Double = 0.0,
    val sicTime: Double = 0.0,
    val soloTime: Double = 0.0,
    val xcTime: Double = 0.0,
    val nightTime: Double = 0.0,
    /** Night-vision-goggle operation time. */
    val nvgTime: Double = 0.0,

    // Instrument conditions (61.51(b)(3))
    val actualInstrumentTime: Double = 0.0,
    val simulatedInstrumentTime: Double = 0.0,
    /** Instrument approaches flown, counted toward 61.57(c) currency. */
    val approachCount: Int = 0,
    /** Holding procedures performed, counted toward the other half of the
     *  61.57(c) instrument currency requirement (along with intercepting
     *  and tracking courses through electronic navigation). */
    val holdCount: Int = 0,

    // Takeoffs and landings
    val dayTakeoffs: Int = 0,
    val dayFullStopLandings: Int = 0,
    /** Day landings that were not to a full stop (touch-and-goes). */
    val dayNonFullStopLandings: Int = 0,
    val nightTakeoffs: Int = 0,
    /** Night landings to a full stop (required for night currency, 61.57(b)). */
    val nightFullStopLandings: Int = 0,
    /** Night landings that were not to a full stop (count for day currency only). */
    val nightNonFullStopLandings: Int = 0,

    // Training
    val dualGivenTime: Double = 0.0,
    val dualReceivedTime: Double = 0.0,
    /** Time in a full flight simulator or flight training device. */
    val simulatorTime: Double = 0.0,
    val groundTrainingGivenTime: Double = 0.0,
    val groundTrainingReceivedTime: Double = 0.0,

    /** Free-form remarks, endorsements, maneuvers performed, etc. */
    val notes: String = ""
) {
    /** "KSNA → KLAX", or a single airport when the entry only has one. */
    val routeDescription: String
        get() = listOf(departureAirport, arrivalAirport)
            .filter { it.isNotEmpty() }
            .joinToString(" → ")

    /** True when the entry has everything 61.51 requires before it can be saved. */
    val isValid: Boolean
        get() = aircraftType.isNotBlank()
            && aircraftRegistration.isNotBlank()
            && departureAirport.isNotBlank()
            && arrivalAirport.isNotBlank()
            && totalTime > 0

    /** Writes this entry's values into a Room `Item` row. */
    fun toItem(): Item = Item(
        id = id,
        date = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        aircraftType = aircraftType,
        category = category?.displayName,
        aircraftClass = aircraftClass?.displayName,
        aircraftRegistration = aircraftRegistration.uppercase(),
        departureAirport = departureAirport.uppercase(),
        arrivalAirport = arrivalAirport.uppercase(),
        totalTime = totalTime,
        picTime = picTime,
        sicTime = sicTime,
        soloTime = soloTime,
        xcTime = xcTime,
        nightTime = nightTime,
        nvgTime = nvgTime,
        actualInstrumentTime = actualInstrumentTime,
        simulatedInstrumentTime = simulatedInstrumentTime,
        approachCount = approachCount,
        holdCount = holdCount,
        dayTakeoffs = dayTakeoffs,
        dayFullStopLandings = dayFullStopLandings,
        dayNonFullStopLandings = dayNonFullStopLandings,
        nightTakeoffs = nightTakeoffs,
        nightFullStopLandings = nightFullStopLandings,
        nightNonFullStopLandings = nightNonFullStopLandings,
        dualGivenTime = dualGivenTime,
        dualReceivedTime = dualReceivedTime,
        simulatorTime = simulatorTime,
        groundTrainingGivenTime = groundTrainingGivenTime,
        groundTrainingReceivedTime = groundTrainingReceivedTime,
        notes = notes
    )

    companion object {
        /** Creates a value copy of a persisted `Item`. */
        fun fromItem(item: Item): FlightEntry = FlightEntry(
            id = item.id,
            date = item.date
                ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                ?: LocalDate.now(),
            aircraftType = item.aircraftType.orEmpty(),
            category = AircraftCategory.fromDisplayName(item.category),
            aircraftClass = AircraftClass.fromDisplayName(item.aircraftClass),
            aircraftRegistration = item.aircraftRegistration.orEmpty(),
            departureAirport = item.departureAirport.orEmpty(),
            arrivalAirport = item.arrivalAirport.orEmpty(),
            totalTime = item.totalTime,
            picTime = item.picTime,
            sicTime = item.sicTime,
            soloTime = item.soloTime,
            xcTime = item.xcTime,
            nightTime = item.nightTime,
            nvgTime = item.nvgTime,
            actualInstrumentTime = item.actualInstrumentTime,
            simulatedInstrumentTime = item.simulatedInstrumentTime,
            approachCount = item.approachCount,
            holdCount = item.holdCount,
            dayTakeoffs = item.dayTakeoffs,
            dayFullStopLandings = item.dayFullStopLandings,
            dayNonFullStopLandings = item.dayNonFullStopLandings,
            nightTakeoffs = item.nightTakeoffs,
            nightFullStopLandings = item.nightFullStopLandings,
            nightNonFullStopLandings = item.nightNonFullStopLandings,
            dualGivenTime = item.dualGivenTime,
            dualReceivedTime = item.dualReceivedTime,
            simulatorTime = item.simulatorTime,
            groundTrainingGivenTime = item.groundTrainingGivenTime,
            groundTrainingReceivedTime = item.groundTrainingReceivedTime,
            notes = item.notes.orEmpty()
        )
    }
}
