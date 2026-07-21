package com.vincentwang.copilot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Room counterpart of PersistenceController (Core Data + CloudKit on iOS).
@Database(
    entities = [Item::class, AircraftProfile::class],
    version = 3,
    exportSchema = false
)
abstract class CopilotDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun aircraftDao(): AircraftDao

    companion object {
        /** v1 → v2: full-logbook columns (category/class, touch-and-go
         *  landings, notes) and the saved-aircraft table. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN aircraftClass TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN notes TEXT")
                db.execSQL(
                    "ALTER TABLE items ADD COLUMN dayNonFullStopLandings INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE items ADD COLUMN nightNonFullStopLandings INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS aircraft (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT, registration TEXT, aircraftType TEXT,
                        category TEXT, aircraftClass TEXT)"""
                )
            }
        }

        /** v2 → v3: instrument approach and hold counts for 61.57(c)
         *  instrument currency. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE items ADD COLUMN approachCount INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE items ADD COLUMN holdCount INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        @Volatile
        private var instance: CopilotDatabase? = null

        fun get(context: Context): CopilotDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CopilotDatabase::class.java,
                    "copilot.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
