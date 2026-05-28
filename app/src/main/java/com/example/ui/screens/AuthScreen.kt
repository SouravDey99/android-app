package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.utils.frostedBackground
import com.example.utils.glassmorphic
import com.example.utils.neonGlow

@Composable
fun AuthScreen(
    currentUsername: String,
    currentAvatar: String,
    onAuthComplete: (String, String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentUsername.ifEmpty { "SyncArtist" }) }
    
    // Modern elegant profiles for collaborative avatars (Unsplash avatars)
    val avatars = listOf(
        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150", // Artist Blue
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150", // Creative Pink
        "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=150", // Tech Slate
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150", // retro golden
        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=150"  // flower purple
    )
    
    var selectedAvatar by remember { mutableStateOf(currentAvatar.ifEmpty { avatars[0] }) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .frostedBackground()
            .testTag("auth_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .glassmorphic(
                    backgroundColor = Color.White.copy(alpha = 0.05f),
                    borderColor = Color.White.copy(alpha = 0.15f),
                    cornerRadius = 24.dp
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Creative Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            
            Text(
                text = "Choose a display identity for wallpaper sync rooms",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Dynamic highlighted Avatar display
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .neonGlow(Color(0xFFD0BCFF), alpha = 0.2f),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedAvatar,
                    contentDescription = "Selected Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFD0BCFF), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar list selection
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                avatars.forEach { url ->
                    val isSelected = selectedAvatar == url
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable { selectedAvatar = url }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Avatar Choice",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Username input
            OutlinedTextField(
                value = nameInput,
                onValueChange = { if (it.length <= 15) nameInput = it },
                label = { Text("Collaborator Name", color = Color.White.copy(alpha = 0.6f)) },
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
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input")
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Auth complete launch button
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onAuthComplete(nameInput, selectedAvatar)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Launch SyncWall",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
