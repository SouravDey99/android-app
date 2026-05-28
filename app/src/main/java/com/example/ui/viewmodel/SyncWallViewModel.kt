package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SyncWallDatabase
import com.example.data.remote.FirebaseManager
import com.example.data.repository.SyncWallRepository
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncWallViewModel(application: Application) : AndroidViewModel(application) {
    private val db = SyncWallDatabase.getDatabase(application)
    private val repository = SyncWallRepository(application, db.roomDao())

    val currentUser = repository.currentUser
    val currentRoom = repository.currentRoom
    val isFirebaseInitialized = FirebaseManager.isInitialized

    // Drawing State Configuration
    private val _selectedTool = MutableStateFlow(ObjectType.DRAW_PENCIL)
    val selectedTool: StateFlow<ObjectType> = _selectedTool

    private val _selectedColor = MutableStateFlow(0xFF00FFCC.toInt()) // Sleek neon teal by default
    val selectedColor: StateFlow<Int> = _selectedColor

    private val _strokeWidth = MutableStateFlow(8f)
    val strokeWidth: StateFlow<Float> = _strokeWidth

    private val _opacity = MutableStateFlow(1f)
    val opacity: StateFlow<Float> = _opacity

    // Layers (max layer counter)
    private var maxLayer = 0

    // Canvas Items and Undo/Redo lists
    val canvasObjects: StateFlow<List<CanvasObject>> = currentRoom
        .flatMapLatest { room ->
            if (room != null) {
                repository.getCanvasObjects(room.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val redoStack = mutableListOf<CanvasObject>()

    init {
        // Initialize Firebase on start
        FirebaseManager.initialize(application)
    }

    fun selectTool(tool: ObjectType) {
        _selectedTool.value = tool
        if (tool == ObjectType.DRAW_PENCIL) {
            _strokeWidth.value = 6f
        } else if (tool == ObjectType.DRAW_BRUSH) {
            _strokeWidth.value = 16f
        } else if (tool == ObjectType.DRAW_NEON) {
            _strokeWidth.value = 14f
        }
    }

    fun selectColor(color: Int) {
        _selectedColor.value = color
    }

    fun setStrokeWidth(width: Float) {
        _strokeWidth.value = width
    }

    fun setOpacity(op: Float) {
        _opacity.value = op
    }

    // Auth Workflow
    fun authenticateUser(username: String, avatarUrl: String) {
        repository.setUsername(username)
        repository.setAvatar(avatarUrl)
    }

    // Room operations
    fun createRoom(name: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val room = repository.createRoom(name)
                onSuccess(room.code)
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to create room")
            }
        }
    }

    fun joinRoom(code: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val room = repository.joinRoom(code)
                if (room != null) {
                    onSuccess()
                } else {
                    onFailure("Room code not found")
                }
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to join room")
            }
        }
    }

    fun leaveRoom() {
        repository.leaveRoom()
        redoStack.clear()
    }

    // Dynamic Firebase adjustments
    fun configureFirebaseCustom(
        apiKey: String,
        projectId: String,
        appId: String,
        senderId: String? = null,
        onResult: (Boolean) -> Unit
    ) {
        val application = getApplication<Application>()
        val success = FirebaseManager.saveCustomCredentials(
            application, apiKey, projectId, appId, senderId
        )
        onResult(success)
    }

    fun clearFirebaseCustom() {
        val application = getApplication<Application>()
        FirebaseManager.clearCustomCredentials(application)
    }

    fun getFirebaseConfig(): Triple<String, String, String> {
        val application = getApplication<Application>()
        return FirebaseManager.getCustomConfig(application)
    }

    // Realtime Drawing Canvas Operations
    fun drawPathAdded(points: List<CanvasPoint>) {
        val room = currentRoom.value ?: return
        val currentUserId = currentUser.value.id
        val type = _selectedTool.value

        maxLayer++
        val obj = CanvasObject(
            roomId = room.id,
            type = type,
            pointsList = points,
            color = if (type == ObjectType.DRAW_NEON) _selectedColor.value else _selectedColor.value,
            strokeWidth = _strokeWidth.value,
            opacity = _opacity.value,
            layer = maxLayer,
            updatedBy = currentUserId
        )

        redoStack.clear() // clear redo stack on new action
        viewModelScope.launch {
            repository.saveCanvasObject(obj)
        }
    }

    fun drawEraserPathAdded(points: List<CanvasPoint>) {
        // Erasing points by adding a special path that has background color (e.g. transparent or dark slate)
        val room = currentRoom.value ?: return
        val currentUserId = currentUser.value.id

        maxLayer++
        val obj = CanvasObject(
            roomId = room.id,
            type = ObjectType.DRAW_BRUSH,
            pointsList = points,
            color = 0xFF0D0E15.toInt(), // Match app-wide sleek canvas deep background
            strokeWidth = _strokeWidth.value * 1.5f,
            opacity = 1f,
            layer = maxLayer,
            updatedBy = currentUserId
        )

        viewModelScope.launch {
            repository.saveCanvasObject(obj)
        }
    }

    fun addTextObject(text: String) {
        val room = currentRoom.value ?: return
        val currentUserId = currentUser.value.id

        maxLayer++
        val obj = CanvasObject(
            roomId = room.id,
            type = ObjectType.TEXT,
            text = text,
            color = _selectedColor.value,
            opacity = _opacity.value,
            x = 350f,
            y = 500f,
            scale = 1.0f,
            layer = maxLayer,
            updatedBy = currentUserId
        )

        viewModelScope.launch {
            repository.saveCanvasObject(obj)
        }
    }

    fun addStickerObject(stickerName: String) {
        val room = currentRoom.value ?: return
        val currentUserId = currentUser.value.id

        maxLayer++
        val obj = CanvasObject(
            roomId = room.id,
            type = ObjectType.IMAGE,
            imageUrl = stickerName,
            x = 300f,
            y = 450f,
            scale = 1.0f,
            layer = maxLayer,
            updatedBy = currentUserId
        )

        viewModelScope.launch {
            repository.saveCanvasObject(obj)
        }
    }

    fun updateObjectTransform(obj: CanvasObject, dx: Float, dy: Float, scaleFactor: Float, dRotation: Float) {
        val updated = obj.copy(
            x = obj.x + dx,
            y = obj.y + dy,
            scale = (obj.scale * scaleFactor).coerceIn(0.25f, 4f),
            rotation = obj.rotation + dRotation,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveCanvasObject(updated)
        }
    }

    fun deleteObject(obj: CanvasObject) {
        viewModelScope.launch {
            repository.deleteCanvasObject(obj.id)
        }
    }

    fun triggerClear() {
        viewModelScope.launch {
            repository.clearCanvas()
        }
    }

    // Undo / Redo Mechanism
    fun triggerUndo() {
        val objects = canvasObjects.value
        val userId = currentUser.value.id
        // Find latest object from current user
        val lastUserObj = objects.findLast { it.updatedBy == userId }
        if (lastUserObj != null) {
            redoStack.add(lastUserObj)
            viewModelScope.launch {
                repository.deleteCanvasObject(lastUserObj.id)
            }
        }
    }

    fun triggerRedo() {
        if (redoStack.isNotEmpty()) {
            val obj = redoStack.removeAt(redoStack.size - 1)
            viewModelScope.launch {
                repository.saveCanvasObject(obj)
            }
        }
    }

    // Apply Real Native Wallpaper
    fun applyWorkspaceWallpaper(bitmap: Bitmap, isHome: Boolean, isLock: Boolean, completionStatus: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.applyWallpaper(bitmap, isHome, isLock)
            withContext(Dispatchers.Main) {
                completionStatus(result)
            }
        }
    }
}
