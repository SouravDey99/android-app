package com.example.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassmorphic(
    backgroundColor: Color = Color.White.copy(alpha = 0.05f),
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    cornerRadius: Dp = 16.dp
): Modifier = this
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(borderColor, borderColor.copy(alpha = 0.02f)),
            start = Offset(0f, 0f),
            end = Offset(100f, 400f)
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)

fun Modifier.frostedBackground(): Modifier = this.drawBehind {
    // 1. Draw central-biased radial dark base gradient
    val bgBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F1115)),
        center = Offset(size.width * 0.5f, size.height * 0.5f),
        radius = size.maxDimension * 0.85f
    )
    drawRect(brush = bgBrush)

    // 2. Soft blurred atmosphere purple highlight (top-left) - matching HTML blur-[80px] bg-purple-500
    val purpleBrush = Brush.radialGradient(
        colors = listOf(Color(0xFFA855F7).copy(alpha = 0.15f), Color.Transparent),
        center = Offset(size.width * 0.25f, size.height * 0.25f),
        radius = size.minDimension * 0.55f
    )
    drawRect(brush = purpleBrush)

    // 3. Soft blurred atmosphere blue highlight (bottom-right) - matching HTML blur-[100px] bg-blue-500
    val blueBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.12f), Color.Transparent),
        center = Offset(size.width * 0.75f, size.height * 0.65f),
        radius = size.minDimension * 0.65f
    )
    drawRect(brush = blueBrush)
}

fun Modifier.neonGlow(
    color: Color = Color(0xFF00FFCC),
    alpha: Float = 0.15f
): Modifier = this.drawBehind {
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = size.minDimension * 0.75f,
        center = Offset(size.width / 2, size.height / 2)
    )
}
