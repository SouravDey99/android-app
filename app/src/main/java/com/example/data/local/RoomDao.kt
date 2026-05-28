package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    // Canvas Objects
    @Query("SELECT * FROM canvas_objects WHERE roomId = :roomId ORDER BY layer ASC, updatedAt ASC")
    fun getCanvasObjectsForRoom(roomId: String): Flow<List<RoomCanvasObject>>

    @Query("SELECT * FROM canvas_objects WHERE id = :objectId LIMIT 1")
    suspend fun getCanvasObjectById(objectId: String): RoomCanvasObject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvasObject(obj: RoomCanvasObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvasObjects(objects: List<RoomCanvasObject>)

    @Query("DELETE FROM canvas_objects WHERE id = :objectId")
    suspend fun deleteCanvasObject(objectId: String)

    @Query("DELETE FROM canvas_objects WHERE roomId = :roomId")
    suspend fun clearCanvasObjectsForRoom(roomId: String)

    // Sync Rooms
    @Query("SELECT * FROM sync_rooms WHERE code = :code LIMIT 1")
    suspend fun getRoomByCode(code: String): RoomSyncRoom?

    @Query("SELECT * FROM sync_rooms WHERE id = :roomId LIMIT 1")
    suspend fun getRoomById(roomId: String): RoomSyncRoom?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomSyncRoom)

    // User Profile
    @Query("SELECT * FROM user_profiles WHERE id = :userId LIMIT 1")
    suspend fun getUserProfile(userId: String): RoomUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(user: RoomUserEntity)

    // Sync Queue (Offline edits)
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getSyncQueue(): List<RoomSyncQueue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToSyncQueue(item: RoomSyncQueue)

    @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
    suspend fun deleteFromSyncQueue(queueId: Int)
}
