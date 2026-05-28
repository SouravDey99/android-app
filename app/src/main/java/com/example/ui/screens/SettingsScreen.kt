package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.frostedBackground
import com.example.utils.glassmorphic
import com.example.utils.neonGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentFirebaseStatus: Boolean,
    onConfigureFirebase: (String, String, String) -> Unit,
    onClearFirebase: () -> Unit,
    getFirebaseValues: () -> Triple<String, String, String>,
    onResetProfile: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val initialValues = remember { getFirebaseValues() }
    var apiKey by remember { mutableStateOf(initialValues.first) }
    var projectId by remember { mutableStateOf(initialValues.second) }
    var appId by remember { mutableStateOf(initialValues.third) }
    
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var activePremiumSection by remember { mutableStateOf<String?>(null) } // "AI", "Couples", "Animated"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .frostedBackground()
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Preferences & Advanced",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Go Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.04f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                
                // 1. Diagnostics Section
                item {
                    Text(
                        text = "DATABASE STATUS",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(
                                backgroundColor = Color.White.copy(alpha = 0.05f),
                                borderColor = Color.White.copy(alpha = 0.15f),
                                cornerRadius = 16.dp
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (currentFirebaseStatus) Color(0xFFD0BCFF) else Color(0xFFFF0055), RoundedCornerShape(5.dp))
                                        .neonGlow(if (currentFirebaseStatus) Color(0xFFD0BCFF) else Color(0xFFFF0055), 0.5f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (currentFirebaseStatus) "Firestore Sync Connection OK" else "Running Mock Offline Sandbox",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Custom configuration controls
                            Text(
                                text = "To sync globally in real-time with another separate device, register your custom Firebase Project parameters below:",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("Web API Key") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFD0BCFF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = projectId,
                                onValueChange = { projectId = it },
                                label = { Text("Project ID") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFD0BCFF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = appId,
                                onValueChange = { appId = it },
                                label = { Text("Application ID") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFD0BCFF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (apiKey.isNotEmpty() && projectId.isNotEmpty() && appId.isNotEmpty()) {
                                            onConfigureFirebase(apiKey, projectId, appId)
                                            infoMessage = "Connected to database custom endpoints!"
                                        } else {
                                            infoMessage = "Error: Fields cannot be empty."
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("Sync Options", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onClearFirebase()
                                        apiKey = ""
                                        projectId = ""
                                        appId = ""
                                        infoMessage = "Cleared custom parameters."
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("Clear DB", fontSize = 12.sp, color = Color.White)
                                }
                            }

                            if (infoMessage != null) {
                                Text(
                                    text = infoMessage!!,
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                            }
                        }
                    }
                }

                // 2. High-Tech Extra Capabilities Section
                item {
                    Text(
                        text = "ADVANCED SYNC FEATURES",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(
                                backgroundColor = Color.White.copy(alpha = 0.05f),
                                borderColor = Color.White.copy(alpha = 0.15f),
                                cornerRadius = 16.dp
                            )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Option A: AI Prompt Sync
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activePremiumSection = if (activePremiumSection == "AI") null else "AI" }
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, "AI", tint = Color(0xFFD0BCFF))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("AI Canvas Prompt Generator", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                }
                                if (activePremiumSection == "AI") {
                                    Text(
                                        text = "Ready to create spectacular visuals with the AI Assistant? In the collaborative editor tab, write your couple design prompts. Our Room listener automatically queries Google's Gemini-3.5-Flash models, rendering matching backgrounds dynamically for you!",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                            // Option B: Couple Scheduled Wallpaper Themes
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activePremiumSection = if (activePremiumSection == "COUPLES") null else "COUPLES" }
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Favorite, "Couples", tint = Color(0xFFD0BCFF))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Scheduled Dual Themes", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                }
                                if (activePremiumSection == "COUPLES") {
                                    Text(
                                        text = "Automate synchronized wallpapers down to specific schedules! Designate an active morning theme (sunrise paint) or a night theme (ambient cosmos). On designated hours, the background engine rotates wallpaper on both your devices simultaneously.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                            // Option C: Interactive Sound/Music Canvas Reactions
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activePremiumSection = if (activePremiumSection == "MUSIC") null else "MUSIC" }
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MusicNote, "Music", tint = Color(0xFFD0BCFF))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Visual Music Reactivity", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                }
                                if (activePremiumSection == "MUSIC") {
                                    Text(
                                        text = "Bring wallpapers to life with sound! Activates local microphone spectrum frequencies. Our editor rendering loops automatically adjust neon glows, line widths, and shadows dynamically matching the ambient track playing in your room.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Reset Profile Section
                item {
                    Button(
                        onClick = { onResetProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("reset_profile_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF0055).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFF0055)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, "Reset")
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Re-establish Collaborator Profile", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
