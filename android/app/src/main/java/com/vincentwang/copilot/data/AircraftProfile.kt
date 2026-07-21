package com.vincentwang.copilot.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Room counterpart of the Core Data "Aircraft" entity: a saved aircraft
// profile the flight form can fill in with one tap.
@Entity(tableName = "aircraft")
data class AircraftProfile(
    @PrimaryKey val id: String,
    /** Optional custom label, e.g. "Club 172". */
    val name: String? = null,
    /** Tail number, e.g. "N12345" — the only required field. */
    val registration: String? = null,
    /** Make and model, e.g. "C172". */
    val aircraftType: String? = null,
    /** FAA category display name, when specified. */
    val category: String? = null,
    /** FAA class display name, when specified. */
    val aircraftClass: String? = null
) {
    /** The label shown wherever a profile is listed: the custom name when
     *  the pilot set one, otherwise the registration. */
    val displayName: String
        get() = name?.takeIf { it.isNotEmpty() } ?: registration ?: "—"

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "registration" to registration,
        "aircraftType" to aircraftType,
        "category" to category,
        "aircraftClass" to aircraftClass
    )

    companion object {
        fun fromMap(data: Map<String, Any?>): AircraftProfile? {
            val id = data["id"] as? String ?: return null
            return AircraftProfile(
                id = id,
                name = data["name"] as? String,
                registration = data["registration"] as? String,
                aircraftType = data["aircraftType"] as? String,
                category = data["category"] as? String,
                aircraftClass = data["aircraftClass"] as? String
            )
        }
    }
}
