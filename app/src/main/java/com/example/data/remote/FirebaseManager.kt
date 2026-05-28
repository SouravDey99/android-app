package com.example.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private const val PREFS_NAME = "SyncWallFirebasePrefs"
    private const val KEY_API_KEY = "apiKey"
    private const val KEY_PROJECT_ID = "projectId"
    private const val KEY_APP_ID = "appId"
    private const val KEY_MESSAGING_SENDER_ID = "messagingSenderId"

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private var cachedAuth: FirebaseAuth? = null
    private var cachedFirestore: FirebaseFirestore? = null

    fun initialize(context: Context) {
        if (_isInitialized.value) return

        try {
            // First check if there's a default Firebase configuration already packaged (e.g. google-services.json)
            FirebaseApp.initializeApp(context)
            Log.d(TAG, "Firebase initialized via default google-services.json")
            setupDefaultClients()
            _isInitialized.value = true
            return
        } catch (e: Exception) {
            Log.w(TAG, "Default Firebase initialization failed (usually missing google-services.json). Trying dynamic preferences.")
        }

        // Try to initialize using saved user credentials (entered via settings screen)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(KEY_API_KEY, null)
        val projectId = prefs.getString(KEY_PROJECT_ID, null)
        val appId = prefs.getString(KEY_APP_ID, null)

        if (!apiKey.isNullOrEmpty() && !projectId.isNullOrEmpty() && !appId.isNullOrEmpty()) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .setGcmSenderId(prefs.getString(KEY_MESSAGING_SENDER_ID, "1035105372")) // default placeholder key
                    .build()

                FirebaseApp.initializeApp(context, options, "SyncWallDynamicApp")
                val dynamicApp = FirebaseApp.getInstance("SyncWallDynamicApp")
                
                cachedAuth = FirebaseAuth.getInstance(dynamicApp)
                cachedFirestore = FirebaseFirestore.getInstance(dynamicApp).apply {
                    firestoreSettings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                }
                
                Log.d(TAG, "Firebase initialized successfully with Custom Dynamic Options")
                _isInitialized.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Dynamic Firebase init failed: ${e.message}")
                _isInitialized.value = false
            }
        } else {
            Log.d(TAG, "No default or custom Firebase credentials found. Running in Offline sandbox mode.")
            _isInitialized.value = false
        }
    }

    private fun setupDefaultClients() {
        try {
            cachedAuth = FirebaseAuth.getInstance()
            cachedFirestore = FirebaseFirestore.getInstance().apply {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate Firebase clients: ${e.message}")
        }
    }

    fun saveCustomCredentials(
        context: Context,
        apiKey: String,
        projectId: String,
        appId: String,
        senderId: String? = null
    ): Boolean {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_PROJECT_ID, projectId)
            .putString(KEY_APP_ID, appId)
            .putString(KEY_MESSAGING_SENDER_ID, senderId)
            .apply()

        // Force re-initialization
        _isInitialized.value = false
        initialize(context)
        return _isInitialized.value
    }

    fun clearCustomCredentials(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        _isInitialized.value = false
        cachedAuth = null
        cachedFirestore = null
    }

    fun getAuth(): FirebaseAuth? = cachedAuth
    fun getFirestore(): FirebaseFirestore? = cachedFirestore

    fun getCustomConfig(context: Context): Triple<String, String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Triple(
            prefs.getString(KEY_API_KEY, "") ?: "",
            prefs.getString(KEY_PROJECT_ID, "") ?: "",
            prefs.getString(KEY_APP_ID, "") ?: ""
        )
    }
}
