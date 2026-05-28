package com.example.domain.model

import java.util.UUID

data class CanvasPoint(val x: Float, val y: Float)

enum class ObjectType {
    DRAW_PENCIL,
    DRAW_BRUSH,
    DRAW_NEON,
    TEXT,
    IMAGE
}

data class CanvasObject(
    val id: String = UUID.randomUUID().toString(),
    val roomId: String = "",
    val type: ObjectType = ObjectType.DRAW_PENCIL,
    val pointsList: List<CanvasPoint> = emptyList(),
    val text: String = "",
    val imageUrl: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val color: Int = 0xFFFFFFFF.toInt(),
    val strokeWidth: Float = 5f,
    val opacity: Float = 1f,
    val layer: Int = 0,
    val updatedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Utility functions to convert points to string and back for database and network
    fun pointsToString(): String {
        return pointsList.joinToString(";") { "${it.x},${it.y}" }
    }

    companion object {
        fun stringToPoints(str: String): List<CanvasPoint> {
            if (str.isEmpty()) return emptyList()
            return str.split(";").mapNotNull {
                val parts = it.split(",")
                if (parts.size == 2) {
                    val x = parts[0].toFloatOrNull()
                    val y = parts[1].toFloatOrNull()
                    if (x != null && y != null) CanvasPoint(x, y) else null
                } else {
                    null
                }
            }
        }
    }
}

data class SyncRoom(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val ownerId: String = "",
    val wallpaperVersion: Int = 0,
    val lastWallpaperUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class RoomUser(
    val id: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val isOnline: Boolean = true
)
