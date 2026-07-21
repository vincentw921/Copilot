package com.vincentwang.copilot

import com.vincentwang.copilot.data.AircraftProfile
import com.vincentwang.copilot.data.Item
import com.vincentwang.copilot.models.AircraftCategory
import com.vincentwang.copilot.models.AircraftClass
import com.vincentwang.copilot.models.AirportDatabase
import com.vincentwang.copilot.models.CsvExporter
import com.vincentwang.copilot.models.FlightEntry
import com.vincentwang.copilot.models.LogbookStats
import com.vincentwang.copilot.models.MonthGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

// Unit tests for the pure logbook logic, ported from CopilotTests.swift:
// totals, 61.57 currency, CSV export, month-grid math, and the
// Firestore/Room mappings.

/** Builds a minimal valid entry, overridable per test. */
private fun makeEntry(
    daysAgo: Long = 0,
    totalTime: Double = 1.0,
    dayTakeoffs: Int = 0,
    dayFullStopLandings: Int = 0,
    nightTakeoffs: Int = 0,
    nightFullStopLandings: Int = 0,
    approachCount: Int = 0,
    holdCount: Int = 0,
    notes: String = ""
) = FlightEntry(
    date = LocalDate.now().minusDays(daysAgo),
    aircraftType = "C172",
    aircraftRegistration = "N12345",
    departureAirport = "KSNA",
    arrivalAirport = "KLAX",
    totalTime = totalTime,
    dayTakeoffs = dayTakeoffs,
    dayFullStopLandings = dayFullStopLandings,
    nightTakeoffs = nightTakeoffs,
    nightFullStopLandings = nightFullStopLandings,
    approachCount = approachCount,
    holdCount = holdCount,
    notes = notes
)

class LogbookStatsTests {

    @Test
    fun totalsSumAcrossEntries() {
        val entries = listOf(
            makeEntry(totalTime = 1.5, dayFullStopLandings = 2),
            makeEntry(totalTime = 2.0, dayFullStopLandings = 1)
        )
        val totals = LogbookStats.totals(entries)
        assertEquals(3.5, totals.totalTime, 1e-9)
        assertEquals(3, totals.dayFullStopLandings)
        assertEquals(2, totals.flightCount)
    }

    @Test
    fun dayCurrencyCountsBothDayAndNightOperations() {
        // 2 day + 1 night takeoff/landing inside 90 days → current for day.
        val entries = listOf(
            makeEntry(daysAgo = 10, dayTakeoffs = 2, dayFullStopLandings = 2),
            makeEntry(daysAgo = 20, nightTakeoffs = 1, nightFullStopLandings = 1)
        )
        val status = LogbookStats.dayCurrency(entries)
        assertEquals(3, status.takeoffs)
        assertEquals(3, status.landings)
        assertTrue(status.isCurrent)
    }

    @Test
    fun dayCurrencyCountsTouchAndGoes() {
        // 61.57(a) doesn't require full stops, so touch-and-goes count.
        val entry = makeEntry(daysAgo = 5, dayTakeoffs = 3)
            .copy(dayNonFullStopLandings = 3)
        val status = LogbookStats.dayCurrency(listOf(entry))
        assertEquals(3, status.landings)
        assertTrue(status.isCurrent)
    }

    @Test
    fun nightCurrencyIgnoresDayAndNonFullStopLandings() {
        val nightTouchAndGoes =
            makeEntry(daysAgo = 20, nightTakeoffs = 2, nightFullStopLandings = 2)
                .copy(nightNonFullStopLandings = 5)
        val entries = listOf(
            makeEntry(daysAgo = 10, dayTakeoffs = 3, dayFullStopLandings = 3),
            nightTouchAndGoes
        )
        val status = LogbookStats.nightCurrency(entries)
        assertEquals(2, status.takeoffs)
        assertEquals(2, status.landings)
        assertFalse(status.isCurrent)
    }

    @Test
    fun currencyExcludesFlightsOlderThan90Days() {
        val entries = listOf(
            makeEntry(daysAgo = 91, dayTakeoffs = 3, dayFullStopLandings = 3)
        )
        val status = LogbookStats.dayCurrency(entries)
        assertEquals(0, status.takeoffs)
        assertFalse(status.isCurrent)
    }

    @Test
    fun instrumentCurrencyRequiresApproachesAndHolds() {
        val entries = listOf(
            makeEntry(daysAgo = 30, approachCount = 4, holdCount = 1),
            makeEntry(daysAgo = 60, approachCount = 2)
        )
        val status = LogbookStats.instrumentCurrency(entries)
        assertEquals(6, status.approaches)
        assertEquals(1, status.holds)
        assertTrue(status.isCurrent)
    }

    @Test
    fun instrumentCurrencyFailsWithoutHolds() {
        // 6 approaches but no holding procedures logged.
        val entries = listOf(makeEntry(daysAgo = 10, approachCount = 6))
        val status = LogbookStats.instrumentCurrency(entries)
        assertEquals(6, status.approaches)
        assertEquals(0, status.holds)
        assertFalse(status.isCurrent)
    }

    @Test
    fun instrumentCurrencyExcludesFlightsOlderThan6Months() {
        val entries = listOf(
            makeEntry(daysAgo = 200, approachCount = 6, holdCount = 2)
        )
        val status = LogbookStats.instrumentCurrency(entries)
        assertEquals(0, status.approaches)
        assertEquals(0, status.holds)
        assertFalse(status.isCurrent)
    }

    @Test
    fun categoryTotalsGroupAndSkipUncategorized() {
        val airplaneSEL = makeEntry(totalTime = 2.0).copy(
            category = AircraftCategory.AIRPLANE,
            aircraftClass = AircraftClass.SINGLE_ENGINE_LAND
        )
        val airplaneSEL2 = makeEntry(totalTime = 1.0).copy(
            category = AircraftCategory.AIRPLANE,
            aircraftClass = AircraftClass.SINGLE_ENGINE_LAND
        )
        val glider = makeEntry(totalTime = 0.5).copy(category = AircraftCategory.GLIDER)
        val uncategorized = makeEntry(totalTime = 3.0)

        val totals = LogbookStats.categoryTotals(
            listOf(airplaneSEL, airplaneSEL2, glider, uncategorized)
        )

        // Uncategorized time never produces a row; same pairs merge.
        assertEquals(2, totals.size)
        assertEquals("Airplane · Single-Engine Land", totals[0].label)
        assertEquals(3.0, totals[0].hours, 1e-9)
        assertEquals("Glider", totals[1].label)
        assertEquals(0.5, totals[1].hours, 1e-9)
    }

    @Test
    fun classHierarchyFollowsCategory() {
        assertTrue(AircraftCategory.AIRPLANE.classes.contains(AircraftClass.SINGLE_ENGINE_LAND))
        assertEquals(
            listOf(AircraftClass.HELICOPTER, AircraftClass.GYROPLANE),
            AircraftCategory.ROTORCRAFT.classes
        )
        assertTrue(AircraftCategory.POWERED_LIFT.classes.isEmpty())
        assertTrue(AircraftCategory.GLIDER.classes.isEmpty())
        assertFalse(AircraftCategory.AIRPLANE.classes.contains(AircraftClass.BALLOON))
    }
}

class CsvExporterTests {

    @Test
    fun csvHasHeaderAndOneRowPerEntry() {
        val csv = CsvExporter.csv(listOf(makeEntry(), makeEntry(daysAgo = 1)))
        val lines = csv.trimEnd().split("\n")
        assertEquals(3, lines.size) // header + 2 rows
        assertTrue(lines[0].startsWith("Date,Aircraft Type,Category,Class,Registration"))
    }

    @Test
    fun csvSortsOldestFirst() {
        val newest = makeEntry(daysAgo = 0)
        val oldest = makeEntry(daysAgo = 30)
        val csv = CsvExporter.csv(listOf(newest, oldest))

        val lines = csv.trimEnd().split("\n")
        assertTrue(lines[1].startsWith(oldest.date.toString()))
        assertTrue(lines[2].startsWith(newest.date.toString()))
    }

    @Test
    fun csvEscapesCommasAndQuotes() {
        val entry = makeEntry(notes = "Steep turns, \"slow flight\"")
        val csv = CsvExporter.csv(listOf(entry))
        assertTrue(csv.contains("\"Steep turns, \"\"slow flight\"\"\""))
    }

    @Test
    fun csvIncludesCrossCountryColumn() {
        val entry = makeEntry(totalTime = 2.0).copy(xcTime = 2.0)
        val csv = CsvExporter.csv(listOf(entry))
        assertTrue(csv.contains("Cross Country"))
        assertTrue(csv.trimEnd().split("\n")[1].contains("2.0"))
    }

    @Test
    fun csvIncludesApproachAndHoldColumns() {
        val entry = makeEntry(approachCount = 3, holdCount = 1)
        val csv = CsvExporter.csv(listOf(entry))
        assertTrue(csv.contains("Approaches,Holds"))
        assertTrue(csv.trimEnd().split("\n")[1].contains("0.0,3,1,0"))
    }

    @Test
    fun entryValidationRequiresCoreFields() {
        assertTrue(makeEntry().isValid)
        assertFalse(makeEntry().copy(arrivalAirport = " ").isValid)
        assertFalse(makeEntry().copy(totalTime = 0.0).isValid)
    }
}

class AirportDatabaseTests {

    @Test
    fun haversineMatchesKnownDistance() {
        // KSNA → KLAX is roughly 31 NM.
        val ksna = AirportDatabase.Coordinate(33.6751, -117.8693)
        val klax = AirportDatabase.Coordinate(33.9425, -118.4080)
        val distance = AirportDatabase.haversineNM(ksna, klax)
        assertTrue(distance > 25 && distance < 40)
    }
}

class AircraftProfileTests {

    @Test
    fun displayNameDefaultsToRegistration() {
        // No custom name → registration is the display name.
        assertEquals("N12345", AircraftProfile(id = "1", registration = "N12345").displayName)
        assertEquals(
            "N12345",
            AircraftProfile(id = "1", name = "", registration = "N12345").displayName
        )
        // A custom name wins once set.
        assertEquals(
            "Club 172",
            AircraftProfile(id = "1", name = "Club 172", registration = "N12345").displayName
        )
    }
}

class MonthGridTests {

    @Test
    fun gridDaysPadsToTheFirstWeekday() {
        // July 1, 2026 is a Wednesday → 3 blanks (Sun, Mon, Tue), 31 days.
        val cells = MonthGrid.gridDays(YearMonth.of(2026, 7), DayOfWeek.SUNDAY)

        assertEquals(3 + 31, cells.size)
        assertTrue(cells.take(3).all { it == null })
        assertEquals(1, cells[3]?.dayOfMonth)
        assertEquals(31, cells.last()?.dayOfMonth)
    }

    @Test
    fun weekdaySymbolsStartAtFirstWeekday() {
        val english = Locale.US
        assertEquals("M", MonthGrid.orderedWeekdaySymbols(DayOfWeek.MONDAY, english).first())
        assertEquals("S", MonthGrid.orderedWeekdaySymbols(DayOfWeek.SUNDAY, english).first())
    }
}

class ItemMappingTests {

    @Test
    fun flightEntryRoundTripsThroughItem() {
        val entry = makeEntry(totalTime = 1.5, dayFullStopLandings = 2).copy(
            category = AircraftCategory.AIRPLANE,
            aircraftClass = AircraftClass.SINGLE_ENGINE_LAND,
            notes = "Round trip"
        )
        assertEquals(entry, FlightEntry.fromItem(entry.toItem()))
    }

    @Test
    fun itemMapRoundTripPreservesAllFields() {
        val item = makeEntry(totalTime = 1.5).copy(
            category = AircraftCategory.ROTORCRAFT,
            aircraftClass = AircraftClass.HELICOPTER,
            notes = "Hover work"
        ).toItem()
        assertEquals(item, Item.fromMap(item.toMap()))
    }

    @Test
    fun fromMapWithoutIdReturnsNull() {
        assertNull(Item.fromMap(mapOf("aircraftType" to "C172")))
    }

    @Test
    fun fromMapHandlesFirestoreNumberTypes() {
        // Firestore returns integers as Long and decimals as Double.
        val item = Item.fromMap(
            mapOf("id" to "x", "date" to 123L, "totalTime" to 2L, "dayTakeoffs" to 3L)
        )
        assertEquals(123L, item?.date)
        assertEquals(2.0, item?.totalTime ?: 0.0, 0.0)
        assertEquals(3, item?.dayTakeoffs)
    }
}
