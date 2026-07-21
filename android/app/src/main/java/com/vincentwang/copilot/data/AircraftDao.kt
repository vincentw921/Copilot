package com.vincentwang.copilot.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AircraftDao {
    // Alphabetical by registration, matching the iOS fetch request.
    @Query("SELECT * FROM aircraft ORDER BY registration ASC")
    fun observeAll(): Flow<List<AircraftProfile>>

    @Upsert
    suspend fun upsertAll(aircraft: List<AircraftProfile>)

    @Query("DELETE FROM aircraft WHERE id = :id")
    suspend fun deleteById(id: String)
}
