package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.frostedBackground
import com.example.utils.glassmorphic
import com.example.utils.neonGlow
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    
    // Pulse animation for logo scale
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteSpec(1500),
        label = "logoScale"
    )

    LaunchedEffect(Unit) {
        delay(2500) // 2.5 seconds showcase
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .frostedBackground()
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Starry particles background decoration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(logoScale)
                    .neonGlow(Color(0xFFD0BCFF), alpha = 0.2f)
                    .glassmorphic(
                        backgroundColor = Color.White.copy(alpha = 0.05f),
                        borderColor = Color.White.copy(alpha = 0.15f),
                        cornerRadius = 32.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Brush,
                    contentDescription = "SyncWall logo",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "SYNCWALL",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color.White,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Realtime Collaborative Wallpapers",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }

        // Elegant bottom credits
        Text(
            text = "DESIGNED FOR CREATIVE COUPLES",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            fontSize = 11.sp,
            color = Color(0xFFFF0055).copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp
        )
    }
}

private fun infiniteSpec(duration: Int): InfiniteRepeatableSpec<Float> {
    return infiniteRepeatable(
        animation = tween(duration, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
}
