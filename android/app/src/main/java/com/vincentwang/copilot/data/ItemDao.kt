package com.vincentwang.copilot.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    // Newest first, matching the iOS @FetchRequest sort descriptor.
    @Query("SELECT * FROM items ORDER BY date DESC")
    fun observeAll(): Flow<List<Item>>

    @Query("SELECT * FROM items ORDER BY date DESC")
    suspend fun getAll(): List<Item>

    @Upsert
    suspend fun upsertAll(items: List<Item>)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM items")
    suspend fun deleteAll()
}
