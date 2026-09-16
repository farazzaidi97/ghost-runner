package com.ghostrunner.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PaceProfileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class GhostRunnerDatabase : RoomDatabase() {

    abstract fun paceProfileDao(): PaceProfileDao

    companion object {
        private const val DB_NAME = "ghost_runner.db"

        @Volatile private var instance: GhostRunnerDatabase? = null

        fun get(context: Context): GhostRunnerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GhostRunnerDatabase::class.java,
                    DB_NAME,
                ).build().also { instance = it }
            }
        }
    }
}
