package com.vincentwang.copilot.models

import android.content.Context
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Offline airport-coordinate lookup and great-circle distance, used to
 * detect cross-country flights (14 CFR 61.1: > 50 NM from the point of
 * departure for most certificate requirements). Ported from
 * AirportDatabase.swift.
 *
 * Data source: the bundled assets/airports.csv, condensed from the
 * public-domain OurAirports dataset (ourairports.com). Each row is
 * "code,lat,lon" and the same airport appears under every identifier a
 * pilot might type (ICAO "KSNA", IATA/local "SNA", etc.).
 */
class AirportDatabase private constructor(private val appContext: Context?) {

    /** A geographic coordinate in degrees. */
    data class Coordinate(val latitude: Double, val longitude: Double)

    /** Identifier → coordinate, loaded lazily from assets. */
    private val airports: Map<String, Coordinate> by lazy { loadAirports() }

    /** Looks up an airport by identifier (case-insensitive), e.g. "KSNA" or "SNA". */
    fun coordinate(identifier: String): Coordinate? =
        airports[identifier.trim().uppercase()]

    /** Great-circle distance in nautical miles between two airports,
     *  or null when either identifier is unknown. */
    fun distanceNM(departure: String, arrival: String): Double? {
        val from = coordinate(departure) ?: return null
        val to = coordinate(arrival) ?: return null
        return haversineNM(from, to)
    }

    /** Parses the bundled "code,lat,lon" CSV into the lookup table. */
    private fun loadAirports(): Map<String, Coordinate> {
        val context = appContext ?: return emptyMap()
        return runCatching {
            context.assets.open("airports.csv").bufferedReader().useLines { lines ->
                val result = HashMap<String, Coordinate>(50_000)
                lines.drop(1).forEach { line -> // skip header
                    val fields = line.split(',')
                    if (fields.size == 3) {
                        val lat = fields[1].toDoubleOrNull()
                        val lon = fields[2].toDoubleOrNull()
                        if (lat != null && lon != null) {
                            result[fields[0]] = Coordinate(lat, lon)
                        }
                    }
                }
                result
            }
        }.getOrDefault(emptyMap())
    }

    companion object {
        /** The distance in nautical miles at which a flight counts as
         *  cross-country for most 61.1(b) purposes. */
        const val CROSS_COUNTRY_THRESHOLD_NM = 50.0

        @Volatile
        private var instance: AirportDatabase? = null

        /** Shared instance; the CSV is parsed once on first lookup. */
        fun get(context: Context): AirportDatabase =
            instance ?: synchronized(this) {
                instance ?: AirportDatabase(context.applicationContext)
                    .also { instance = it }
            }

        /** Haversine great-circle distance in nautical miles. */
        fun haversineNM(from: Coordinate, to: Coordinate): Double {
            val earthRadiusNM = 3440.065
            val lat1 = Math.toRadians(from.latitude)
            val lat2 = Math.toRadians(to.latitude)
            val dLat = lat2 - lat1
            val dLon = Math.toRadians(to.longitude - from.longitude)

            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
            return earthRadiusNM * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
