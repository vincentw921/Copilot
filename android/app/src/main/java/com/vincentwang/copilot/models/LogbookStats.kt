package com.vincentwang.copilot.models

import java.time.LocalDate

// Pure aggregation over logbook entries: career totals and FAR 61.57
// recent-experience (currency) checks. Ported from LogbookStats.swift.

/** Career totals summed across a set of logbook entries. */
data class LogbookTotals(
    var totalTime: Double = 0.0,
    var picTime: Double = 0.0,
    var sicTime: Double = 0.0,
    var soloTime: Double = 0.0,
    var xcTime: Double = 0.0,
    var nightTime: Double = 0.0,
    var nvgTime: Double = 0.0,
    var actualInstrumentTime: Double = 0.0,
    var simulatedInstrumentTime: Double = 0.0,
    var dualGivenTime: Double = 0.0,
    var dualReceivedTime: Double = 0.0,
    var simulatorTime: Double = 0.0,
    var groundTrainingGivenTime: Double = 0.0,
    var groundTrainingReceivedTime: Double = 0.0,
    var dayTakeoffs: Int = 0,
    var dayFullStopLandings: Int = 0,
    var dayNonFullStopLandings: Int = 0,
    var nightTakeoffs: Int = 0,
    var nightFullStopLandings: Int = 0,
    var nightNonFullStopLandings: Int = 0,
    /** Number of flights included in these totals. */
    var flightCount: Int = 0
)

/** Result of a 14 CFR 61.57 passenger-carrying currency check. */
data class CurrencyStatus(
    /** Takeoffs performed within the 90-day window. */
    val takeoffs: Int,
    /** Qualifying landings within the 90-day window (any landing for day
     *  currency; full-stop only for night). */
    val landings: Int
) {
    /** True when both counts meet the required 3 within the preceding 90 days. */
    val isCurrent: Boolean get() = takeoffs >= 3 && landings >= 3
}

/** Total time flown in one category (and class, when logged). */
data class CategoryClassTotal(
    val category: AircraftCategory,
    val aircraftClass: AircraftClass?,
    val hours: Double
) {
    /** "Airplane · Single-Engine Land", or just the category when no
     *  class was logged. */
    val label: String
        get() = aircraftClass?.let { "${category.displayName} · ${it.displayName}" }
            ?: category.displayName
}

/** Stateless calculators over `List<FlightEntry>`. */
object LogbookStats {
    /** Sums every logged column across `entries`. */
    fun totals(entries: List<FlightEntry>): LogbookTotals {
        val t = LogbookTotals()
        for (e in entries) {
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

    /** Total time per category/class pair, most-flown first. Entries with
     *  no category (or no time) are skipped, so zero rows never appear. */
    fun categoryTotals(entries: List<FlightEntry>): List<CategoryClassTotal> {
        val hours = mutableMapOf<String, CategoryClassTotal>()
        for (e in entries) {
            val category = e.category ?: continue
            if (e.totalTime <= 0) continue
            val key = "${category.displayName}|${e.aircraftClass?.displayName.orEmpty()}"
            val current = hours[key] ?: CategoryClassTotal(category, e.aircraftClass, 0.0)
            hours[key] = current.copy(hours = current.hours + e.totalTime)
        }
        return hours.values.sortedByDescending { it.hours }
    }

    /** Day passenger-carrying currency per 61.57(a): 3 takeoffs and
     *  3 landings within the preceding 90 days. Day and night operations
     *  both count, and the landings need not be to a full stop. */
    fun dayCurrency(entries: List<FlightEntry>, asOf: LocalDate = LocalDate.now()): CurrencyStatus {
        val recent = entries.filter { it.date >= windowStart(asOf) }
        return CurrencyStatus(
            takeoffs = recent.sumOf { it.dayTakeoffs + it.nightTakeoffs },
            landings = recent.sumOf {
                it.dayFullStopLandings + it.dayNonFullStopLandings +
                    it.nightFullStopLandings + it.nightNonFullStopLandings
            }
        )
    }

    /** Night passenger-carrying currency per 61.57(b): 3 takeoffs and
     *  3 full-stop landings at night within the preceding 90 days. */
    fun nightCurrency(entries: List<FlightEntry>, asOf: LocalDate = LocalDate.now()): CurrencyStatus {
        val recent = entries.filter { it.date >= windowStart(asOf) }
        return CurrencyStatus(
            takeoffs = recent.sumOf { it.nightTakeoffs },
            landings = recent.sumOf { it.nightFullStopLandings }
        )
    }

    /** The start of the preceding-90-day window used by 61.57. */
    private fun windowStart(asOf: LocalDate): LocalDate = asOf.minusDays(90)
}
