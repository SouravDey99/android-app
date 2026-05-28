package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "canvas_objects")
data class RoomCanvasObject(
    @PrimaryKey val id: String,
    val roomId: String,
    val type: String, // mapped from ObjectType enum name
    val pointsString: String, // serialized points
    val text: String,
    val imageUrl: String,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
    val color: Int,
    val strokeWidth: Float,
    val opacity: Float,
    val layer: Int,
    val updatedBy: String,
    val updatedAt: Long
)

@Entity(tableName = "sync_rooms")
data class RoomSyncRoom(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val ownerId: String,
    val wallpaperVersion: Int,
    val lastWallpaperUrl: String,
    val createdAt: Long
)

@Entity(tableName = "user_profiles")
data class RoomUserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val avatarUrl: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class RoomSyncQueue(
    @PrimaryKey(autoGenerate = true) val queueId: Int = 0,
    val objectId: String,
    val roomId: String,
    val operationType: String, // "INSERT", "UPDATE", "DELETE"
    val timestamp: Long = System.currentTimeMillis()
)
