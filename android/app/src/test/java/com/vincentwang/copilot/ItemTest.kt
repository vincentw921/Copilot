package com.vincentwang.copilot

import com.vincentwang.copilot.data.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Android counterpart of CopilotTests.swift.
class ItemTest {

    @Test
    fun mapRoundTripPreservesAllFields() {
        val item = Item(
            id = "abc-123",
            date = 1_726_000_000_000,
            aircraftType = "C172",
            aircraftRegistration = "N1234AB",
            departureAirport = "KSNA",
            arrivalAirport = "KLAX",
            totalTime = 1.5,
            picTime = 1.5,
            dayTakeoffs = 2,
            dayFullStopLandings = 2
        )

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
            mapOf(
                "id" to "x",
                "date" to 123L,
                "totalTime" to 2L,
                "dayTakeoffs" to 3L
            )
        )
        assertEquals(123L, item?.date)
        assertEquals(2.0, item?.totalTime ?: 0.0, 0.0)
        assertEquals(3, item?.dayTakeoffs)
    }
}
