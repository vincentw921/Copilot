package com.vincentwang.copilot.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Room counterpart of the Core Data "Item" entity in Copilot.xcdatamodeld.
// One row per logbook entry; times are decimal hours, dates epoch millis.
@Entity(tableName = "items")
data class Item(
    @PrimaryKey val id: String,
    val date: Long? = null,
    val aircraftType: String? = null,
    /** FAA category display name ("Airplane"), when specified. */
    val category: String? = null,
    /** FAA class display name ("Single-Engine Land"), when specified. */
    val aircraftClass: String? = null,
    val aircraftRegistration: String? = null,
    val departureAirport: String? = null,
    val arrivalAirport: String? = null,
    val totalTime: Double = 0.0,
    val picTime: Double = 0.0,
    val sicTime: Double = 0.0,
    val soloTime: Double = 0.0,
    val nightTime: Double = 0.0,
    val xcTime: Double = 0.0,
    val actualInstrumentTime: Double = 0.0,
    val simulatedInstrumentTime: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val approachCount: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val holdCount: Int = 0,
    val simulatorTime: Double = 0.0,
    val nvgTime: Double = 0.0,
    val dualGivenTime: Double = 0.0,
    val dualReceivedTime: Double = 0.0,
    val groundTrainingGivenTime: Double = 0.0,
    val groundTrainingReceivedTime: Double = 0.0,
    val dayTakeoffs: Int = 0,
    val nightTakeoffs: Int = 0,
    val dayFullStopLandings: Int = 0,
    val nightFullStopLandings: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val dayNonFullStopLandings: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val nightNonFullStopLandings: Int = 0,
    val notes: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "date" to date,
        "aircraftType" to aircraftType,
        "category" to category,
        "aircraftClass" to aircraftClass,
        "aircraftRegistration" to aircraftRegistration,
        "departureAirport" to departureAirport,
        "arrivalAirport" to arrivalAirport,
        "totalTime" to totalTime,
        "picTime" to picTime,
        "sicTime" to sicTime,
        "soloTime" to soloTime,
        "nightTime" to nightTime,
        "xcTime" to xcTime,
        "actualInstrumentTime" to actualInstrumentTime,
        "simulatedInstrumentTime" to simulatedInstrumentTime,
        "approachCount" to approachCount,
        "holdCount" to holdCount,
        "simulatorTime" to simulatorTime,
        "nvgTime" to nvgTime,
        "dualGivenTime" to dualGivenTime,
        "dualReceivedTime" to dualReceivedTime,
        "groundTrainingGivenTime" to groundTrainingGivenTime,
        "groundTrainingReceivedTime" to groundTrainingReceivedTime,
        "dayTakeoffs" to dayTakeoffs,
        "nightTakeoffs" to nightTakeoffs,
        "dayFullStopLandings" to dayFullStopLandings,
        "nightFullStopLandings" to nightFullStopLandings,
        "dayNonFullStopLandings" to dayNonFullStopLandings,
        "nightNonFullStopLandings" to nightNonFullStopLandings,
        "notes" to notes
    )

    companion object {
        fun fromMap(data: Map<String, Any?>): Item? {
            val id = data["id"] as? String ?: return null
            fun double(key: String) = (data[key] as? Number)?.toDouble() ?: 0.0
            fun int(key: String) = (data[key] as? Number)?.toInt() ?: 0
            return Item(
                id = id,
                date = (data["date"] as? Number)?.toLong(),
                aircraftType = data["aircraftType"] as? String,
                category = data["category"] as? String,
                aircraftClass = data["aircraftClass"] as? String,
                aircraftRegistration = data["aircraftRegistration"] as? String,
                departureAirport = data["departureAirport"] as? String,
                arrivalAirport = data["arrivalAirport"] as? String,
                totalTime = double("totalTime"),
                picTime = double("picTime"),
                sicTime = double("sicTime"),
                soloTime = double("soloTime"),
                nightTime = double("nightTime"),
                xcTime = double("xcTime"),
                actualInstrumentTime = double("actualInstrumentTime"),
                simulatedInstrumentTime = double("simulatedInstrumentTime"),
                approachCount = int("approachCount"),
                holdCount = int("holdCount"),
                simulatorTime = double("simulatorTime"),
                nvgTime = double("nvgTime"),
                dualGivenTime = double("dualGivenTime"),
                dualReceivedTime = double("dualReceivedTime"),
                groundTrainingGivenTime = double("groundTrainingGivenTime"),
                groundTrainingReceivedTime = double("groundTrainingReceivedTime"),
                dayTakeoffs = int("dayTakeoffs"),
                nightTakeoffs = int("nightTakeoffs"),
                dayFullStopLandings = int("dayFullStopLandings"),
                nightFullStopLandings = int("nightFullStopLandings"),
                dayNonFullStopLandings = int("dayNonFullStopLandings"),
                nightNonFullStopLandings = int("nightNonFullStopLandings"),
                notes = data["notes"] as? String
            )
        }
    }
}
