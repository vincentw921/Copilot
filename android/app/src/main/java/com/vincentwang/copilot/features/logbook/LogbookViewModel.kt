package com.vincentwang.copilot.features.logbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vincentwang.copilot.data.AircraftProfile
import com.vincentwang.copilot.data.Item
import com.vincentwang.copilot.data.ItemRepository
import com.vincentwang.copilot.models.AircraftCategory
import com.vincentwang.copilot.models.AircraftClass
import com.vincentwang.copilot.models.AirportDatabase
import com.vincentwang.copilot.models.FlightEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

/**
 * Shared view model over the logbook store, used by every tab (the
 * Compose equivalent of the iOS screens sharing one Core Data view
 * context via @FetchRequest).
 */
class LogbookViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ItemRepository.get(app)

    /** All entries, newest first (the DAO orders by date DESC). */
    val items: StateFlow<List<Item>> = repository.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Saved aircraft profiles, alphabetical by registration. */
    val aircraft: StateFlow<List<AircraftProfile>> = repository.aircraft
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Equivalent of listening for NSPersistentStoreRemoteChange: fold
        // remote Firestore changes back into the local store.
        repository.startRemoteSync()
    }

    /** Persists a new or edited entry; sync happens automatically. */
    fun saveEntry(entry: FlightEntry) {
        viewModelScope.launch { repository.upsert(listOf(entry.toItem())) }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch { repository.delete(item) }
    }

    fun saveAircraft(profile: AircraftProfile) {
        viewModelScope.launch { repository.upsertAircraft(profile) }
    }

    fun deleteAircraft(profile: AircraftProfile) {
        viewModelScope.launch { repository.deleteAircraft(profile) }
    }

    // MARK: Developer utilities (debug builds), ported from ProfileView.swift.

    /** Seeds a varied set of realistic entries — different categories and
     *  classes (SEL, MEL, SES, helicopter, glider), a cross-country leg,
     *  and a night flight — so every screen has something to show. */
    fun insertSampleData() {
        data class Sample(
            val type: String, val registration: String,
            val category: AircraftCategory, val aircraftClass: AircraftClass?,
            val from: String, val to: String,
            val hours: Double, val isNight: Boolean
        )

        val samples = listOf(
            Sample("C172", "N735AB", AircraftCategory.AIRPLANE,
                AircraftClass.SINGLE_ENGINE_LAND, "KSNA", "KLAX", 1.1, false),
            Sample("BE76", "N4478C", AircraftCategory.AIRPLANE,
                AircraftClass.MULTI_ENGINE_LAND, "KSNA", "KSBP", 2.4, false),
            Sample("ICON A5", "N882BA", AircraftCategory.AIRPLANE,
                AircraftClass.SINGLE_ENGINE_SEA, "KAVX", "KSNA", 1.3, false),
            Sample("R44", "N7204Q", AircraftCategory.ROTORCRAFT,
                AircraftClass.HELICOPTER, "KSNA", "KSNA", 0.8, false),
            Sample("ASK 21", "N321GL", AircraftCategory.GLIDER,
                null, "KHMT", "KHMT", 0.9, false),
            Sample("C172", "N735AB", AircraftCategory.AIRPLANE,
                AircraftClass.SINGLE_ENGINE_LAND, "KSNA", "KCRQ", 1.5, true)
        )

        val airports = AirportDatabase.get(getApplication())
        val entries = samples.mapIndexed { index, sample ->
            val distance = airports.distanceNM(sample.from, sample.to)
            val isCrossCountry =
                distance != null && distance >= AirportDatabase.CROSS_COUNTRY_THRESHOLD_NM
            val dayTakeoffs = if (sample.isNight) 0 else Random.nextInt(1, 4)

            FlightEntry(
                date = LocalDate.now().minusDays(index * 9L),
                aircraftType = sample.type,
                aircraftRegistration = sample.registration,
                category = sample.category,
                aircraftClass = sample.aircraftClass,
                departureAirport = sample.from,
                arrivalAirport = sample.to,
                totalTime = sample.hours,
                picTime = sample.hours,
                // Log cross-country time the same way the form would.
                xcTime = if (isCrossCountry) sample.hours else 0.0,
                nightTime = if (sample.isNight) sample.hours else 0.0,
                nightTakeoffs = if (sample.isNight) 3 else 0,
                nightFullStopLandings = if (sample.isNight) 3 else 0,
                // Pattern work: every takeoff but the last is a touch-and-go.
                dayTakeoffs = dayTakeoffs,
                dayFullStopLandings = if (sample.isNight) 0 else 1,
                dayNonFullStopLandings = if (sample.isNight) 0 else dayTakeoffs - 1,
                notes = "Sample flight"
            )
        }
        viewModelScope.launch { repository.upsert(entries.map { it.toItem() }) }
    }

    /** Removes every logbook entry. */
    fun deleteAllData() {
        viewModelScope.launch { repository.deleteAllItems() }
    }

    override fun onCleared() {
        repository.stopRemoteSync()
    }
}
