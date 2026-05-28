package com.example.data.repository

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.example.data.local.*
import com.example.data.remote.FirebaseManager
import com.example.domain.model.*
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resumeWithException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.random.Random

class SyncWallRepository(
    private val context: Context,
    private val roomDao: RoomDao
) {
    private val TAG = "SyncWallRepository"
    private val repositoryScope = CoroutineScope(Dispatchers.Main + Job())

    // Tracks current room
    private val _currentRoom = MutableStateFlow<SyncRoom?>(null)
    val currentRoom: StateFlow<SyncRoom?> = _currentRoom

    // Active Firestore snapshot listener
    private var canvasListener: ListenerRegistration? = null
    private var roomListener: ListenerRegistration? = null

    // Track active user
    private val _currentUser = MutableStateFlow<RoomUser>(
        RoomUser(
            id = "user_${Random.nextInt(1000, 9999)}",
            username = "CreativeArtist",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150"
        )
    )
    val currentUser: StateFlow<RoomUser> = _currentUser

    // Local simulation job for multi-user buddy edits in offline sandbox mode
    private var buddyJob: Job? = null

    init {
        // Observe offline queue and attempt sync periodically if Firestore becomes active
        repositoryScope.launch {
            while (isActive) {
                delay(8000)
                if (FirebaseManager.isInitialized.value) {
                    processOfflineSyncQueue()
                }
            }
        }
    }

    fun setUsername(name: String) {
        _currentUser.value = _currentUser.value.copy(username = name)
        // Store in local DB also
        repositoryScope.launch {
            roomDao.insertUserProfile(
                RoomUserEntity(
                    id = _currentUser.value.id,
                    username = name,
                    avatarUrl = _currentUser.value.avatarUrl
                )
            )
        }
    }

    fun setAvatar(url: String) {
        _currentUser.value = _currentUser.value.copy(avatarUrl = url)
        repositoryScope.launch {
            roomDao.insertUserProfile(
                RoomUserEntity(
                    id = _currentUser.value.id,
                    username = _currentUser.value.username,
                    avatarUrl = url
                )
            )
        }
    }

    // Flow of canvas objects from Room database
    fun getCanvasObjects(roomId: String): Flow<List<CanvasObject>> {
        return roomDao.getCanvasObjectsForRoom(roomId).map { list ->
            list.map {
                CanvasObject(
                    id = it.id,
                    roomId = it.roomId,
                    type = ObjectType.valueOf(it.type),
                    pointsList = CanvasObject.stringToPoints(it.pointsString),
                    text = it.text,
                    imageUrl = it.imageUrl,
                    x = it.x,
                    y = it.y,
                    scale = it.scale,
                    rotation = it.rotation,
                    color = it.color,
                    strokeWidth = it.strokeWidth,
                    opacity = it.opacity,
                    layer = it.layer,
                    updatedBy = it.updatedBy,
                    updatedAt = it.updatedAt
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun createRoom(roomName: String): SyncRoom = withContext(Dispatchers.IO) {
        val code = generateRoomCode()
        val roomId = UUID.randomUUID().toString()
        val ownerId = _currentUser.value.id

        val newRoom = SyncRoom(
            id = roomId,
            code = code,
            name = roomName,
            ownerId = ownerId,
            wallpaperVersion = 1,
            createdAt = System.currentTimeMillis()
        )

        // Save locally
        roomDao.insertRoom(
            RoomSyncRoom(
                id = newRoom.id,
                code = newRoom.code,
                name = newRoom.name,
                ownerId = newRoom.ownerId,
                wallpaperVersion = newRoom.wallpaperVersion,
                lastWallpaperUrl = newRoom.lastWallpaperUrl,
                createdAt = newRoom.createdAt
            )
        )

        // Sync with Firestore if active
        val db = FirebaseManager.getFirestore()
        if (db != null) {
            try {
                val roomMap = hashMapOf(
                    "roomId" to roomId,
                    "roomCode" to code,
                    "name" to roomName,
                    "ownerId" to ownerId,
                    "wallpaperVersion" to 1,
                    "lastWallpaperUrl" to "",
                    "createdAt" to newRoom.createdAt
                )
                db.collection("rooms").document(roomId).set(roomMap)
                
                // Add roomMember row
                val memberMap = hashMapOf(
                    "roomId" to roomId,
                    "userId" to ownerId,
                    "joinedAt" to System.currentTimeMillis()
                )
                db.collection("roomMembers").document("${roomId}_${ownerId}").set(memberMap)
                Log.d(TAG, "Room synchronized on Firestore: $code")
            } catch (e: Exception) {
                Log.e(TAG, "Firestore createRoom failed: ${e.message}")
                // Fallback queued
                roomDao.addToSyncQueue(RoomSyncQueue(objectId = roomId, roomId = roomId, operationType = "CREATE_ROOM"))
            }
        }

        _currentRoom.value = newRoom
        startListeningToRoom(newRoom.id)
        
        // Start offline buddy simulator to show collaborative feature
        if (db == null) {
            startBuddySimulation(roomId)
        }

        return@withContext newRoom
    }

    suspend fun joinRoom(code: String): SyncRoom? = withContext(Dispatchers.IO) {
        val db = FirebaseManager.getFirestore()
        var room: SyncRoom? = null

        if (db != null) {
            try {
                val snapshot = db.collection("rooms")
                    .whereEqualTo("roomCode", code.uppercase())
                    .get()
                    .awaitTask() // customized await extension to avoid compile-time issues with Task.await()

                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    room = SyncRoom(
                        id = doc.getString("roomId") ?: "",
                        code = doc.getString("roomCode") ?: "",
                        name = doc.getString("name") ?: "Collaborative Canvas",
                        ownerId = doc.getString("ownerId") ?: "",
                        wallpaperVersion = doc.getLong("wallpaperVersion")?.toInt() ?: 1,
                        lastWallpaperUrl = doc.getString("lastWallpaperUrl") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore joinRoom query failed: ${e.message}")
            }
        }

        // If not found online (or Firestore offline), look up locally
        if (room == null) {
            val localRoom = roomDao.getRoomByCode(code.uppercase())
            if (localRoom != null) {
                room = SyncRoom(
                    id = localRoom.id,
                    code = localRoom.code,
                    name = localRoom.name,
                    ownerId = localRoom.ownerId,
                    wallpaperVersion = localRoom.wallpaperVersion,
                    lastWallpaperUrl = localRoom.lastWallpaperUrl,
                    createdAt = localRoom.createdAt
                )
            } else {
                // Generate a mockup room for test sandbox to keep demo clean
                val mockId = UUID.randomUUID().toString()
                room = SyncRoom(
                    id = mockId,
                    code = code.uppercase(),
                    name = "Collaborative Lounge",
                    ownerId = "host_1928",
                    wallpaperVersion = 1,
                    createdAt = System.currentTimeMillis()
                )
                roomDao.insertRoom(
                    RoomSyncRoom(
                        id = room.id,
                        code = room.code,
                        name = room.name,
                        ownerId = room.ownerId,
                        wallpaperVersion = room.wallpaperVersion,
                        lastWallpaperUrl = room.lastWallpaperUrl,
                        createdAt = room.createdAt
                    )
                )
            }
        }

        room?.let {
            _currentRoom.value = it
            startListeningToRoom(it.id)

            // Auto enroll user in roomMembers on Firestore
            if (db != null) {
                try {
                    val memberMap = hashMapOf(
                        "roomId" to it.id,
                        "userId" to _currentUser.value.id,
                        "joinedAt" to System.currentTimeMillis()
                    )
                    db.collection("roomMembers").document("${it.id}_${_currentUser.value.id}").set(memberMap)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to register room member: ${e.message}")
                }
            } else {
                startBuddySimulation(it.id)
            }
        }

        return@withContext room
    }

    fun leaveRoom() {
        buddyJob?.cancel()
        buddyJob = null
        canvasListener?.remove()
        canvasListener = null
        roomListener?.remove()
        roomListener = null
        _currentRoom.value = null
    }

    private fun startListeningToRoom(roomId: String) {
        val db = FirebaseManager.getFirestore()
        if (db == null) return

        // Stop previous listeners
        canvasListener?.remove()
        roomListener?.remove()

        // 1. Listen to Canvas Changes
        canvasListener = db.collection("canvasObjects")
            .whereEqualTo("roomId", roomId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Canvas Firestore snapshot listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    repositoryScope.launch(Dispatchers.IO) {
                        for (change in snapshots.documentChanges) {
                            val doc = change.document
                            val objectId = doc.id
                            
                            // Extract metadata
                            val creator = doc.getString("updatedBy") ?: ""
                            // Optimistic update check: skip if updated by self unless it is deleted or forced
                            if (creator == _currentUser.value.id && change.type != DocumentChange.Type.REMOVED) {
                                continue
                            }

                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val dbObj = RoomCanvasObject(
                                        id = objectId,
                                        roomId = roomId,
                                        type = doc.getString("type") ?: "DRAW_PENCIL",
                                        pointsString = doc.getString("pointsString") ?: "",
                                        text = doc.getString("text") ?: "",
                                        imageUrl = doc.getString("imageUrl") ?: "",
                                        x = doc.getDouble("x")?.toFloat() ?: 0f,
                                        y = doc.getDouble("y")?.toFloat() ?: 0f,
                                        scale = doc.getDouble("scale")?.toFloat() ?: 1f,
                                        rotation = doc.getDouble("rotation")?.toFloat() ?: 0f,
                                        color = doc.getLong("color")?.toInt() ?: 0xFFFFFFFF.toInt(),
                                        strokeWidth = doc.getDouble("strokeWidth")?.toFloat() ?: 5f,
                                        opacity = doc.getDouble("opacity")?.toFloat() ?: 1f,
                                        layer = doc.getLong("layer")?.toInt() ?: 0,
                                        updatedBy = creator,
                                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                                    )
                                    roomDao.insertCanvasObject(dbObj)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    roomDao.deleteCanvasObject(objectId)
                                }
                            }
                        }
                    }
                }
            }

        // 2. Listen to Room Version changes (for applying remote wallpapers)
        roomListener = db.collection("rooms").document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val currentVer = _currentRoom.value?.wallpaperVersion ?: 0
                val serverVer = snapshot.getLong("wallpaperVersion")?.toInt() ?: 0
                val versionIncremented = serverVer > currentVer && serverVer > 1

                val updatedRoom = _currentRoom.value?.copy(
                    wallpaperVersion = serverVer,
                    lastWallpaperUrl = snapshot.getString("lastWallpaperUrl") ?: ""
                ) ?: return@addSnapshotListener
                
                _currentRoom.value = updatedRoom

                // If wallpaper is applied remotely, we should download/retrieve and apply locally too!
                if (versionIncremented) {
                    repositoryScope.launch(Dispatchers.IO) {
                        applyRemoteWallpaper(roomId, serverVer)
                    }
                }
            }
    }

    suspend fun saveCanvasObject(obj: CanvasObject) = withContext(Dispatchers.IO) {
        val updatedObj = obj.copy(
            roomId = _currentRoom.value?.id ?: "",
            updatedBy = _currentUser.value.id,
            updatedAt = System.currentTimeMillis()
        )

        // Save local Room db (Optimistic UX update)
        roomDao.insertCanvasObject(
            RoomCanvasObject(
                id = updatedObj.id,
                roomId = updatedObj.roomId,
                type = updatedObj.type.name,
                pointsString = updatedObj.pointsToString(),
                text = updatedObj.text,
                imageUrl = updatedObj.imageUrl,
                x = updatedObj.x,
                y = updatedObj.y,
                scale = updatedObj.scale,
                rotation = updatedObj.rotation,
                color = updatedObj.color,
                strokeWidth = updatedObj.strokeWidth,
                opacity = updatedObj.opacity,
                layer = updatedObj.layer,
                updatedBy = updatedObj.updatedBy,
                updatedAt = updatedObj.updatedAt
            )
        )

        // Sync Firestore
        val db = FirebaseManager.getFirestore()
        if (db != null) {
            try {
                val map = hashMapOf(
                    "id" to updatedObj.id,
                    "roomId" to updatedObj.roomId,
                    "type" to updatedObj.type.name,
                    "pointsString" to updatedObj.pointsToString(),
                    "text" to updatedObj.text,
                    "imageUrl" to updatedObj.imageUrl,
                    "x" to updatedObj.x,
                    "y" to updatedObj.y,
                    "scale" to updatedObj.scale,
                    "rotation" to updatedObj.rotation,
                    "color" to updatedObj.color,
                    "strokeWidth" to updatedObj.strokeWidth,
                    "opacity" to updatedObj.opacity,
                    "layer" to updatedObj.layer,
                    "updatedBy" to updatedObj.updatedBy,
                    "updatedAt" to updatedObj.updatedAt
                )
                db.collection("canvasObjects").document(updatedObj.id).set(map)
            } catch (e: Exception) {
                Log.e(TAG, "Firestore write object failed: ${e.message}")
                roomDao.addToSyncQueue(
                    RoomSyncQueue(
                        objectId = updatedObj.id,
                        roomId = updatedObj.roomId,
                        operationType = "UPSERT"
                    )
                )
            }
        }
    }

    suspend fun deleteCanvasObject(objectId: String) = withContext(Dispatchers.IO) {
        val currentRoomId = _currentRoom.value?.id ?: ""
        roomDao.deleteCanvasObject(objectId)

        val db = FirebaseManager.getFirestore()
        if (db != null) {
            try {
                db.collection("canvasObjects").document(objectId).delete()
            } catch (e: Exception) {
                roomDao.addToSyncQueue(
                    RoomSyncQueue(
                        objectId = objectId,
                        roomId = currentRoomId,
                        operationType = "DELETE"
                    )
                )
            }
        }
    }

    suspend fun clearCanvas() = withContext(Dispatchers.IO) {
        val currentRoomId = _currentRoom.value?.id ?: return@withContext
        roomDao.clearCanvasObjectsForRoom(currentRoomId)

        val db = FirebaseManager.getFirestore()
        if (db != null) {
            try {
                val snapshot = db.collection("canvasObjects")
                    .whereEqualTo("roomId", currentRoomId)
                    .get()
                    .awaitTask()
                db.runBatch { batch ->
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Clear canvas Firestore batch failed: ${e.message}")
            }
        }
    }

    // Apply local Wallpaper
    suspend fun applyWallpaper(bitmap: Bitmap, isHome: Boolean, isLock: Boolean): Boolean = withContext(Dispatchers.IO) {
        val currentRoomId = _currentRoom.value?.id ?: return@withContext false
        var successLocal = false

        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (isHome) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                }
                if (isLock) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            successLocal = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply wallpaper of size ${bitmap.width}x${bitmap.height}: ${e.message}")
        }

        if (successLocal) {
            // Save the bitmap file to local folder cache so that other local/simulated users can read it from file,
            // or update the version.
            val file = File(context.filesDir, "synced_wallpaper_${currentRoomId}.png")
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                    out.flush()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed caching bitmap locally: ${e.message}")
            }

            // Increment Room version with remote update
            val newVer = (_currentRoom.value?.wallpaperVersion ?: 1) + 1
            val updated = _currentRoom.value?.copy(wallpaperVersion = newVer, lastWallpaperUrl = file.absolutePath)
            _currentRoom.value = updated

            // Perform DB room update
            if (updated != null) {
                roomDao.insertRoom(
                    RoomSyncRoom(
                        id = updated.id,
                        code = updated.code,
                        name = updated.name,
                        ownerId = updated.ownerId,
                        wallpaperVersion = updated.wallpaperVersion,
                        lastWallpaperUrl = updated.lastWallpaperUrl,
                        createdAt = updated.createdAt
                    )
                )

                // Sync wallpaper applying action
                val db = FirebaseManager.getFirestore()
                if (db != null) {
                    try {
                        db.collection("rooms").document(updated.id).update(
                            mapOf(
                                "wallpaperVersion" to updated.wallpaperVersion,
                                "lastWallpaperUrl" to "FILE://${file.name}" // Mock representation of uploaded file
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Update wallpaper version failed on Fire: ${e.message}")
                    }
                }
            }
        }

        return@withContext successLocal
    }

    private suspend fun applyRemoteWallpaper(roomId: String, version: Int) {
        Log.d(TAG, "Remote peer triggered Apply Wallpaper! Applying locally. Version $version")
        val file = File(context.filesDir, "synced_wallpaper_${roomId}.png")
        if (file.exists()) {
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                try {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    wallpaperManager.setBitmap(bitmap)
                    Log.d(TAG, "Successfully applied remote synced wallpaper locally!")
                } catch (e: Exception) {
                    Log.e(TAG, "Applying remote wallpaper bitmap failed: ${e.message}")
                }
            }
        }
    }

    // Process local cache upload when connection is back online
    private suspend fun processOfflineSyncQueue() {
        val queue = roomDao.getSyncQueue()
        if (queue.isEmpty()) return

        val db = FirebaseManager.getFirestore() ?: return
        Log.d(TAG, "Syncing offline changes (${queue.size} items) to Firestore")

        for (item in queue) {
            try {
                if (item.operationType == "UPSERT") {
                    val entity = roomDao.getCanvasObjectById(item.objectId)
                    if (entity != null) {
                        val map = hashMapOf(
                            "id" to entity.id,
                            "roomId" to entity.roomId,
                            "type" to entity.type,
                            "pointsString" to entity.pointsString,
                            "text" to entity.text,
                            "imageUrl" to entity.imageUrl,
                            "x" to entity.x,
                            "y" to entity.y,
                            "scale" to entity.scale,
                            "rotation" to entity.rotation,
                            "color" to entity.color,
                            "strokeWidth" to entity.strokeWidth,
                            "opacity" to entity.opacity,
                            "layer" to entity.layer,
                            "updatedBy" to entity.updatedBy,
                            "updatedAt" to entity.updatedAt
                        )
                        db.collection("canvasObjects").document(entity.id).set(map)
                    }
                } else if (item.operationType == "DELETE") {
                    db.collection("canvasObjects").document(item.objectId).delete()
                }
                
                // Clear from queue
                roomDao.deleteFromSyncQueue(item.queueId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing item ${item.queueId}: ${e.message}")
                break // Wait for next cycle
            }
        }
    }

    // Active companion bot simulator for offline sandbox mode
    // Drawing elements so that user can experience bidirectional collaborative canvas instantly!
    private fun startBuddySimulation(roomId: String) {
        buddyJob?.cancel()
        buddyJob = repositoryScope.launch(Dispatchers.IO) {
            delay(4000) // greet after joining
            val buddyId = "buddy_bot_42"
            val buddyName = "VibeBuddy"

            // 1. Draw a neon brush smile or shapes sequentially
            while (isActive) {
                delay(Random.nextLong(10000, 18000))
                val actions = listOf(
                    "DRAW",
                    "TEXT",
                    "STAMP"
                )
                val chosenAction = actions.random()
                when (chosenAction) {
                    "DRAW" -> {
                        // Drawing path of points
                        val points = mutableListOf<CanvasPoint>()
                        val centerX = 200f + Random.nextInt(200)
                        val centerY = 300f + Random.nextInt(400)
                        
                        // Circle loop path
                        for (i in 0..10) {
                            val angle = (i * 36) * Math.PI / 180.0
                            val r = 40f
                            val px = (centerX + r * Math.cos(angle)).toFloat()
                            val py = (centerY + r * Math.sin(angle)).toFloat()
                            points.add(CanvasPoint(px, py))
                        }

                        val mockDraw = CanvasObject(
                            id = "buddy_draw_${Random.nextInt(100000)}",
                            roomId = roomId,
                            type = listOf(ObjectType.DRAW_NEON, ObjectType.DRAW_PENCIL, ObjectType.DRAW_BRUSH).random(),
                            pointsList = points,
                            color = listOf(0xFF00FFCC.toInt(), 0xFFFF0055.toInt(), 0xFFFFFF00.toInt(), 0xFF9933FF.toInt()).random(),
                            strokeWidth = 14f,
                            updatedBy = buddyId,
                            updatedAt = System.currentTimeMillis()
                        )
                        roomDao.insertCanvasObject(
                            RoomCanvasObject(
                                id = mockDraw.id,
                                roomId = roomId,
                                type = mockDraw.type.name,
                                pointsString = mockDraw.pointsToString(),
                                text = "",
                                imageUrl = "",
                                x = 0f,
                                y = 0f,
                                scale = 1f,
                                rotation = 0f,
                                color = mockDraw.color,
                                strokeWidth = mockDraw.strokeWidth,
                                opacity = 1f,
                                layer = 2,
                                updatedBy = buddyId,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    "TEXT" -> {
                        val greetings = listOf("Hey from VibeBuddy!", "Luminous Wallpaper ✨", "Dynamic Dual Canvas!", "Collab Canvas 🚀")
                        val mockText = CanvasObject(
                            id = "buddy_text_${Random.nextInt(100000)}",
                            roomId = roomId,
                            type = ObjectType.TEXT,
                            text = greetings.random(),
                            color = listOf(0xFFFFFFFF.toInt(), 0xFFFF00FF.toInt(), 0xFFFFFF00.toInt()).random(),
                            x = 100f + Random.nextInt(150),
                            y = 150f + Random.nextInt(400),
                            scale = 1.25f,
                            updatedBy = buddyId,
                            updatedAt = System.currentTimeMillis()
                        )
                        roomDao.insertCanvasObject(
                            RoomCanvasObject(
                                id = mockText.id,
                                roomId = roomId,
                                type = mockText.type.name,
                                pointsString = "",
                                text = mockText.text,
                                imageUrl = "",
                                x = mockText.x,
                                y = mockText.y,
                                scale = mockText.scale,
                                rotation = mockText.rotation,
                                color = mockText.color,
                                strokeWidth = 5f,
                                opacity = 1f,
                                layer = 3,
                                updatedBy = buddyId,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    "STAMP" -> {
                        // Place sticker
                        val stamps = listOf("ic_brush", "ic_sticker_star", "ic_sticker_heart")
                        val mockSticker = CanvasObject(
                            id = "buddy_stamp_${Random.nextInt(100000)}",
                            roomId = roomId,
                            type = ObjectType.IMAGE,
                            imageUrl = stamps.random(),
                            x = 120f + Random.nextInt(200),
                            y = 200f + Random.nextInt(350),
                            scale = 1.0f,
                            updatedBy = buddyId,
                            updatedAt = System.currentTimeMillis()
                        )
                        roomDao.insertCanvasObject(
                            RoomCanvasObject(
                                id = mockSticker.id,
                                roomId = roomId,
                                type = mockSticker.type.name,
                                pointsString = "",
                                text = "",
                                imageUrl = mockSticker.imageUrl,
                                x = mockSticker.x,
                                y = mockSticker.y,
                                scale = mockSticker.scale,
                                rotation = mockSticker.rotation,
                                color = mockSticker.color,
                                strokeWidth = 0f,
                                opacity = 1f,
                                layer = 1,
                                updatedBy = buddyId,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }

    private fun generateRoomCode(): String {
        val uppercaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val codeLength = 6
        return (1..codeLength)
            .map { uppercaseChars[Random.nextInt(uppercaseChars.length)] }
            .joinToString("")
    }

    // Custom Task await extension to prevent kotlinx-coroutines-play-services dependency mismatch
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result, null)
            } else {
                cont.resumeWithException(task.exception ?: RuntimeException("Firebase Task failed"))
            }
        }
    }
}
