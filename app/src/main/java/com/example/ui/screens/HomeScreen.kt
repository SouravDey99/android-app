package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.RoomUser
import com.example.domain.model.SyncRoom
import com.example.utils.frostedBackground
import com.example.utils.glassmorphic
import com.example.utils.neonGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: RoomUser,
    isFirebaseOnline: Boolean,
    onNavigateToCreateJoin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onQuickJoinRoom: (String) -> Unit
) {
    // Local historical or active test rooms for fast selection in demo
    val mockSessionRooms = listOf(
        SyncRoom("room_0", "HEART8", "My Sweetheart Lobe", "user_1", 2, "", System.currentTimeMillis() - 8640000),
        SyncRoom("room_1", "CREA7E", "Luminous Lounge", "user_2", 8, "", System.currentTimeMillis() - 360000000)
    )

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
                            text = "SyncWall",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFFD0BCFF), CircleShape)
                                .clickable { onNavigateToSettings() }
                        ) {
                            AsyncImage(
                                model = currentUser.avatarUrl,
                                contentDescription = "My Profile Icon",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { onNavigateToSettings() },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings Icon",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.04f)
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToCreateJoin() },
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72),
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(8.dp)
                        .testTag("create_join_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create or Join Room Link",
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Connection Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                    // decoration
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(
                                backgroundColor = if (isFirebaseOnline) {
                                    Color(0xFF00FFCC).copy(alpha = 0.08f)
                                } else {
                                    Color(0xFFFF0055).copy(alpha = 0.08f)
                                },
                                borderColor = if (isFirebaseOnline) {
                                    Color(0xFF00FFCC).copy(alpha = 0.25f)
                                } else {
                                    Color(0xFFFF0055).copy(alpha = 0.25f)
                                },
                                cornerRadius = 16.dp
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isFirebaseOnline) Color(0xFF00FFCC) else Color(0xFFFF0055))
                                .neonGlow(if (isFirebaseOnline) Color(0xFF00FFCC) else Color(0xFFFF0055), 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = if (isFirebaseOnline) "Live Firestore Online" else "Offline Sandbox Space",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = if (isFirebaseOnline) {
                                    "Connected instantly to global database rooms"
                                } else {
                                    "Running safely with room DB cache & companion bot"
                                },
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // 2. Main Stats Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .glassmorphic(
                                backgroundColor = Color(0xFF161826).copy(0.6f),
                                borderColor = Color.White.copy(0.08f),
                                cornerRadius = 16.dp
                            )
                            .padding(16.dp),
                    ) {
                        Column {
                            Icon(Icons.Default.CloudSync, "Rooms", tint = Color(0xFF00FFCC))
                            Text(
                                text = "Lobby Rooms",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "Active",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .glassmorphic(
                                backgroundColor = Color(0xFF161826).copy(0.6f),
                                borderColor = Color.White.copy(0.08f),
                                cornerRadius = 16.dp
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Icon(Icons.Default.Wallpaper, "Syncs", tint = Color(0xFFFF0055))
                            Text(
                                text = "Synced",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "Instant",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 3. Saved & Recent Lobby list label
            item {
                Text(
                    text = "HISTORICAL COLLABS",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            // 4. Lobby Item Cards
            items(mockSessionRooms) { room ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickJoinRoom(room.code) }
                        .glassmorphic(
                            backgroundColor = Color(0xFF131522).copy(0.8f),
                            borderColor = Color.White.copy(0.12f),
                            cornerRadius = 16.dp
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF00FFCC).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = room.code,
                                color = Color(0xFF00FFCC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = room.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Applied count: ${room.wallpaperVersion} wallpapers",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Join Room Icon",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Empty state helper tip
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💡 Tip: Connecting on different devices? Use the same Room Code to sync changes instantly!",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
}
