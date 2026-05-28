package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.frostedBackground
import com.example.utils.glassmorphic
import com.example.utils.neonGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (String) -> Unit,
    onNavigateBack: () -> Unit,
    roomError: String? = null
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = JOIN, 1 = CREATE
    var roomNameInput by remember { mutableStateOf("") }
    var roomCodeInput by remember { mutableStateOf("") }
    
    val focusManager = LocalFocusManager.current

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
                            text = if (activeTab == 0) "Join Luminous Lobby" else "Create Co-Space",
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(
                            backgroundColor = Color.White.copy(alpha = 0.05f),
                            borderColor = Color.White.copy(alpha = 0.15f),
                            cornerRadius = 24.dp
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tab Selection Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(Color(0xFF0C0D16), RoundedCornerShape(25.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    color = if (activeTab == 0) Color(0xFFD0BCFF).copy(alpha = 0.15f) else Color.Transparent,
                                    shape = RoundedCornerShape(21.dp)
                                )
                                .clickable { activeTab = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JOIN ROOM",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 0) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    color = if (activeTab == 1) Color(0xFFD0BCFF).copy(alpha = 0.15f) else Color.Transparent,
                                    shape = RoundedCornerShape(21.dp)
                                )
                                .clickable { activeTab = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CREATE ROOM",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 1) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Error prompt
                    if (!roomError.isNullOrEmpty()) {
                        Text(
                            text = roomError,
                            color = Color(0xFFFF0055),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                    }

                    // TAB 0: JOIN ROOM
                    if (activeTab == 0) {
                        Text(
                            text = "Enter the 6-character room code shared by your partner:",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = {
                                if (it.length <= 6) roomCodeInput = it.uppercase()
                            },
                            label = { Text("6-Digit Lobby Code") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = Color(0xFF0E101A).copy(alpha = 0.4f),
                                unfocusedContainerColor = Color(0xFF0E101A).copy(alpha = 0.2f)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (roomCodeInput.length == 6) {
                                    onJoinRoom(roomCodeInput)
                                }
                            }),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = 4.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("room_code_input")
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        Button(
                            onClick = {
                                if (roomCodeInput.length == 6) {
                                    onJoinRoom(roomCodeInput)
                                }
                            },
                            enabled = roomCodeInput.length == 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("join_room_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.MeetingRoom, "Join Lobby")
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Connect Canvas", fontWeight = FontWeight.Bold)
                        }

                    } else {
                        // TAB 1: CREATE ROOM
                        Text(
                            text = "Initialize a brand-new synchronized lobby for mutual drawing:",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        OutlinedTextField(
                            value = roomNameInput,
                            onValueChange = { roomNameInput = it },
                            label = { Text("Lobby Name (e.g. Dream Lab)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = Color(0xFF0E101A).copy(alpha = 0.4f),
                                unfocusedContainerColor = Color(0xFF0E101A).copy(alpha = 0.2f)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (roomNameInput.isNotBlank()) {
                                    onCreateRoom(roomNameInput)
                                }
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("room_name_input")
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        Button(
                            onClick = {
                                if (roomNameInput.isNotBlank()) {
                                    onCreateRoom(roomNameInput)
                                }
                            },
                            enabled = roomNameInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("create_room_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Add, "Create Lobby")
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Establish Lobby", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
