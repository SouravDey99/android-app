package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RoomCanvasObject::class,
        RoomSyncRoom::class,
        RoomUserEntity::class,
        RoomSyncQueue::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SyncWallDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao

    companion object {
        @Volatile
        private var INSTANCE: SyncWallDatabase? = null

        fun getDatabase(context: Context): SyncWallDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SyncWallDatabase::class.java,
                    "syncwall_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
