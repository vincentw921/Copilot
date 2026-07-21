package com.vincentwang.copilot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room counterpart of PersistenceController (Core Data + CloudKit on iOS).
@Database(entities = [Item::class], version = 1, exportSchema = false)
abstract class CopilotDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var instance: CopilotDatabase? = null

        fun get(context: Context): CopilotDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CopilotDatabase::class.java,
                    "copilot.db"
                ).build().also { instance = it }
            }
    }
}
