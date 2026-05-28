package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CanvasObject
import com.example.domain.model.CanvasPoint
import com.example.domain.model.ObjectType
import com.example.ui.viewmodel.SyncWallViewModel
import com.example.utils.frostedBackground
import com.example.utils.glassmorphic
import com.example.utils.neonGlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: SyncWallViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val room by viewModel.currentRoom.collectAsStateWithLifecycle()
    val objects by viewModel.canvasObjects.collectAsStateWithLifecycle()
    
    val selectedTool by viewModel.selectedTool.collectAsStateWithLifecycle()
    val selectedColor by viewModel.selectedColor.collectAsStateWithLifecycle()
    val strokeWidth by viewModel.strokeWidth.collectAsStateWithLifecycle()
    val opacity by viewModel.opacity.collectAsStateWithLifecycle()

    // Interactive canvas draw states
    val currentPath = remember { mutableStateListOf<CanvasPoint>() }
    var selectedObject by remember { mutableStateOf<CanvasObject?>(null) }
    
    // UI Panels toggles
    var showApplyDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showStickerDialog by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var applyStatusMessage by remember { mutableStateOf<String?>(null) }
    var isApplying by remember { mutableStateOf(false) }

    val presetColors = listOf(
        0xFF00FFCC.toInt(), // Neon Teal
        0xFFFF0055.toInt(), // Vibrant Pink
        0xFFFFFF00.toInt(), // Glowing Yellow
        0xFF9933FF.toInt(), // Deep Purple
        0xFF0088FF.toInt(), // Electric Blue
        0xFFFFFFFF.toInt(), // Solid White
        0xFF1D2030.toInt()  // Dark Slate
    )

    // Layout configuration sizing
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .frostedBackground()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = room?.name ?: "Collaborative Canvas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Lobby PIN: ${room?.code ?: "------"}",
                                fontSize = 11.sp,
                                color = Color(0xFFD0BCFF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.leaveRoom()
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        // Quick Action Shortcuts: Undo, Redo, Clear
                        IconButton(onClick = { viewModel.triggerUndo() }) {
                            Icon(Icons.Default.Undo, "Undo", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.triggerRedo() }) {
                            Icon(Icons.Default.Redo, "Redo", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.triggerClear() }) {
                            Icon(Icons.Default.Refresh, "Reset Board", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.04f)
                    )
                )
            },
            bottomBar = {
                // Elegant Frosted Toolbox Controls Shelf
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(
                            backgroundColor = Color.White.copy(alpha = 0.05f),
                            borderColor = Color.White.copy(alpha = 0.15f),
                            cornerRadius = 24.dp
                        )
                        .padding(16.dp)
                ) {
                    // Opacity & Stroke density control rows when drawing active
                    if (selectedTool == ObjectType.DRAW_PENCIL || selectedTool == ObjectType.DRAW_BRUSH || selectedTool == ObjectType.DRAW_NEON) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Size", color = Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.width(36.dp))
                            Slider(
                                value = strokeWidth,
                                onValueChange = { viewModel.setStrokeWidth(it) },
                                valueRange = 2f..40f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD0BCFF),
                                    activeTrackColor = Color(0xFFD0BCFF)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Opacity", color = Color.White.copy(0.6f), fontSize = 11.sp, modifier = Modifier.width(42.dp))
                            Slider(
                                value = opacity,
                                onValueChange = { viewModel.setOpacity(it) },
                                valueRange = 0.1f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD0BCFF),
                                    activeTrackColor = Color(0xFFD0BCFF)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Primary Tool select, Stickers, and Apply button row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tool categories selection bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Brush Selection Options
                            listOf(
                                Triple(ObjectType.DRAW_PENCIL, Icons.Default.Create, "Pencil"),
                                Triple(ObjectType.DRAW_BRUSH, Icons.Default.Brush, "Brush"),
                                Triple(ObjectType.DRAW_NEON, Icons.Default.AutoAwesome, "Neon")
                            ).forEach { (tool, icon, desc) ->
                                val active = selectedTool == tool
                                IconButton(
                                    onClick = { viewModel.selectTool(tool) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            color = if (active) Color(0xFFD0BCFF).copy(alpha = 0.15f) else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = if (active) 1.dp else 0.dp,
                                            color = if (active) Color(0xFFD0BCFF) else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Icon(icon, desc, tint = if (active) Color(0xFFD0BCFF) else Color.White.copy(0.6f))
                                }
                            }

                            // Text Tool Launcher
                            IconButton(
                                onClick = { showTextDialog = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color.White.copy(0.04f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.TextFields, "Add Text", tint = Color.White)
                            }

                            // Shared stamp pack selector dialog
                            IconButton(
                                onClick = { showStickerDialog = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color.White.copy(0.04f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.Celebration, "Sticker", tint = Color.White)
                            }

                            // Colors Palette Trigger Drawer
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(selectedColor))
                                    .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(10.dp))
                                    .clickable { showColorPicker = !showColorPicker }
                            )
                        }

                        // Applying real Wallpaper sync trigger button
                        Button(
                            onClick = { showApplyDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("apply_wallpaper_trigger")
                        ) {
                            Icon(Icons.Default.Wallpaper, "Apply")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Collapsible palette color selections
                    AnimatedVisibility(visible = showColorPicker) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            presetColors.forEach { colorVal ->
                                val act = selectedColor == colorVal
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorVal))
                                        .border(
                                            width = if (act) 2.dp else 1.dp,
                                            color = if (act) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.selectColor(colorVal)
                                        }
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
            
            // Visual Ambient Grid backplane decoration
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpace = 40.dp.toPx()
                for (x in 0..size.width.toInt() step gridSpace.toInt()) {
                    drawLine(
                        Color.White.copy(alpha = 0.03f),
                        Offset(x.toFloat(), 0f),
                        Offset(x.toFloat(), size.height),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..size.height.toInt() step gridSpace.toInt()) {
                    drawLine(
                        Color.White.copy(alpha = 0.03f),
                        Offset(0f, y.toFloat()),
                        Offset(size.width, y.toFloat()),
                        strokeWidth = 1f
                    )
                }
            }

            // PRIMARY COLLABORATIVE CANVAS VIEW
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("collaborative_canvas")
                    .pointerInput(selectedTool) {
                        // Drawing path trigger gesture
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath.clear()
                                currentPath.add(CanvasPoint(offset.x, offset.y))
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentPath.add(CanvasPoint(change.position.x, change.position.y))
                            },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    viewModel.drawPathAdded(currentPath.toList())
                                    currentPath.clear()
                                }
                            }
                        )
                    }
            ) {
                // 1. Render historic synchronized database objects
                objects.forEach { obj ->
                    when (obj.type) {
                        ObjectType.DRAW_PENCIL, ObjectType.DRAW_BRUSH, ObjectType.DRAW_NEON -> {
                            if (obj.pointsList.size > 1) {
                                val path = Path().apply {
                                    val start = obj.pointsList.first()
                                    moveTo(start.x, start.y)
                                    for (i in 1 until obj.pointsList.size) {
                                        val point = obj.pointsList[i]
                                        lineTo(point.x, point.y)
                                    }
                                }
                                
                                when (obj.type) {
                                    ObjectType.DRAW_NEON -> {
                                        // Radiant backing neon blur
                                        drawPath(
                                            path = path,
                                            color = Color(obj.color),
                                            alpha = obj.opacity * 0.4f,
                                            style = Stroke(
                                                width = obj.strokeWidth * 2.5f,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                        // Solid radiant core line
                                        drawPath(
                                            path = path,
                                            color = Color.White,
                                            alpha = obj.opacity,
                                            style = Stroke(
                                                width = obj.strokeWidth * 0.7f,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                    else -> {
                                        drawPath(
                                            path = path,
                                            color = Color(obj.color),
                                            alpha = obj.opacity,
                                            style = Stroke(
                                                width = obj.strokeWidth,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        ObjectType.TEXT -> {
                            // Draw Text using Android canvas native draw
                            drawIntoCanvas { canvas ->
                                val paint = AndroidPaint().apply {
                                    color = obj.color
                                    textSize = 24f * obj.scale * density
                                    isAntiAlias = true
                                    textAlign = AndroidPaint.Align.CENTER
                                    // shadow glow effect
                                    setShadowLayer(12f, 0f, 0f, obj.color)
                                }
                                canvas.save()
                                canvas.translate(obj.x, obj.y)
                                canvas.rotate(obj.rotation)
                                canvas.nativeCanvas.drawText(obj.text, 0f, 0f, paint)
                                canvas.restore()
                            }
                        }
                        ObjectType.IMAGE -> {
                            // Predefined custom shapes/stickers (star, heart, paint brush stamp)
                            drawIntoCanvas { canvas ->
                                canvas.save()
                                canvas.translate(obj.x, obj.y)
                                canvas.rotate(obj.rotation)
                                
                                val paint = AndroidPaint().apply {
                                    color = Color(0xFFFF0055).toArgb() // Default sticker brand color
                                    this.strokeWidth = 4f
                                    isAntiAlias = true
                                }
                                
                                if (obj.imageUrl == "ic_sticker_heart") {
                                    // Custom Heart vector drawing using native paths
                                    val hrtPath = android.graphics.Path().apply {
                                        moveTo(0f, -15f)
                                        cubicTo(-25f, -40f, -50f, -10f, 0f, 30f)
                                        cubicTo(50f, -10f, 25f, -40f, 0f, -15f)
                                    }
                                    paint.color = Color(0xFFFF0055).toArgb()
                                    paint.style = AndroidPaint.Style.FILL
                                    canvas.nativeCanvas.drawPath(hrtPath, paint)
                                } else if (obj.imageUrl == "ic_sticker_star") {
                                    // Star sketch vector
                                    val starPath = android.graphics.Path()
                                    val rOut = 30f * obj.scale
                                    val rIn = 12f * obj.scale
                                    for (i in 0 until 10) {
                                        val angle = i * Math.PI / 5
                                        val r = if (i % 2 == 0) rOut else rIn
                                        val px = (r * sin(angle)).toFloat()
                                        val py = (-r * cos(angle)).toFloat()
                                        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                                    }
                                    starPath.close()
                                    paint.color = Color(0xFFFFFF00).toArgb()
                                    paint.style = AndroidPaint.Style.FILL
                                    canvas.nativeCanvas.drawPath(starPath, paint)
                                } else {
                                    // Default Brush Stamp shape
                                    val brushPath = android.graphics.Path().apply {
                                        moveTo(-10f, -10f)
                                        lineTo(10f, -10f)
                                        lineTo(20f, 15f)
                                        lineTo(-20f, 15f)
                                        close()
                                    }
                                    paint.color = Color(0xFF00FFCC).toArgb()
                                    paint.style = AndroidPaint.Style.FILL
                                    canvas.nativeCanvas.drawPath(brushPath, paint)
                                }
                                canvas.restore()
                            }
                        }
                    }
                }

                // 2. Render user's active drawn stroke before insertion
                if (currentPath.size > 1) {
                    val path = Path().apply {
                        val start = currentPath.first()
                        moveTo(start.x, start.y)
                        for (i in 1 until currentPath.size) {
                            val point = currentPath[i]
                            lineTo(point.x, point.y)
                        }
                    }
                    if (selectedTool == ObjectType.DRAW_NEON) {
                        drawPath(
                            path = path,
                            color = Color(selectedColor),
                            alpha = opacity * 0.4f,
                            style = Stroke(width = strokeWidth * 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = path,
                            color = Color.White,
                            alpha = opacity,
                            style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    } else {
                        drawPath(
                            path = path,
                            color = Color(selectedColor),
                            alpha = opacity,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            // 3. Selection overlays & transformer bounding boxes for Texts/Stickers
            objects.filter { it.type == ObjectType.TEXT || it.type == ObjectType.IMAGE }.forEach { obj ->
                val isSelected = selectedObject?.id == obj.id
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = (obj.x / density).dp - 60.dp,
                            y = (obj.y / density).dp - 50.dp
                        )
                        .size(120.dp, 80.dp)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) Color(0xFF00FFCC) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .pointerInput(obj) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                viewModel.updateObjectTransform(obj, pan.x, pan.y, zoom, rotation)
                            }
                        }
                        .clickable {
                            selectedObject = if (isSelected) null else obj
                        }
                ) {
                    if (isSelected) {
                        // Quick floating transforms option: Delete trigger
                        IconButton(
                            onClick = {
                                viewModel.deleteObject(obj)
                                selectedObject = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color(0xFFFF0055), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Object",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // DIALOG: Shared Sticker stamp Pack
            if (showStickerDialog) {
                Dialog(onDismissRequest = { showStickerDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(
                                backgroundColor = Color(0xFF141524),
                                borderColor = Color.White.copy(0.15f),
                                cornerRadius = 20.dp
                            )
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Pre-packaged Synced Stamps", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                listOf(
                                    Pair("ic_sticker_heart", "❤️ Heart"),
                                    Pair("ic_sticker_star", "⭐ Star"),
                                    Pair("ic_sticker_brush", "🎨 Brush")
                                ).forEach { (id, label) ->
                                    Button(
                                        onClick = {
                                            viewModel.addStickerObject(id)
                                            showStickerDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f))
                                    ) {
                                        Text(label, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // DIALOG: Custom Text Field Dialog
            if (showTextDialog) {
                var textInput by remember { mutableStateOf("") }
                Dialog(onDismissRequest = { showTextDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(
                                backgroundColor = Color(0xFF141524),
                                borderColor = Color.White.copy(0.15f),
                                cornerRadius = 20.dp
                            )
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Type collaborative text:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FFCC)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("add_text_input")
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    if (textInput.isNotEmpty()) {
                                        viewModel.addTextObject(textInput)
                                        showTextDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color(0xFF0D0E15)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Insert on Canvas", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // DIALOG: Apply Wallpaper Destination picking dialogue
            if (showApplyDialog) {
                Dialog(onDismissRequest = { 
                    showApplyDialog = false 
                    applyStatusMessage = null
                }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(
                                backgroundColor = Color(0xFF141524),
                                borderColor = Color.White.copy(0.15f),
                                cornerRadius = 20.dp
                            )
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Synchronize Device Wallpaper",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Applies your jointly designed canvas locally and notifies your partner's device to automatically sync!",
                                fontSize = 11.sp,
                                color = Color.White.copy(0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                            )

                            if (applyStatusMessage != null) {
                                Text(
                                    text = applyStatusMessage!!,
                                    color = if (applyStatusMessage!!.contains("Success")) Color(0xFF00FFCC) else Color(0xFFFF0055),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }

                            // Selection Rows
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple(true, false, "Home"),
                                    Triple(false, true, "Lock"),
                                    Triple(true, true, "Both Screen")
                                ).forEach { (isHome, isLock, text) ->
                                    Button(
                                        onClick = {
                                            isApplying = true
                                            applyStatusMessage = "Synthesizing wallpaper..."
                                            
                                            // 1. Create a workspace bitmap of parent canvas size
                                            val w = (screenWidth.value * density).toInt()
                                            val h = (screenHeight.value * density).toInt()
                                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                            val canvas = AndroidCanvas(bitmap)
                                            
                                            // Draw slate canvas background
                                            canvas.drawColor(0xFF0D0E15.toInt())
                                            
                                            // 2. Draw all CanvasObjects on this bitmap
                                            objects.forEach { obj ->
                                                val paint = AndroidPaint().apply {
                                                    color = obj.color
                                                    this.strokeWidth = obj.strokeWidth * density
                                                    isAntiAlias = true
                                                    strokeCap = AndroidPaint.Cap.ROUND
                                                    strokeJoin = AndroidPaint.Join.ROUND
                                                }
                                                
                                                when (obj.type) {
                                                    ObjectType.DRAW_PENCIL, ObjectType.DRAW_BRUSH, ObjectType.DRAW_NEON -> {
                                                        if (obj.pointsList.size > 1) {
                                                            val path = android.graphics.Path()
                                                            path.moveTo(obj.pointsList.first().x, obj.pointsList.first().y)
                                                            for (i in 1 until obj.pointsList.size) {
                                                                path.lineTo(obj.pointsList[i].x, obj.pointsList[i].y)
                                                            }
                                                            if (obj.type == ObjectType.DRAW_NEON) {
                                                                // Neon backdrop draw
                                                                paint.strokeWidth = obj.strokeWidth * 2.5f * density
                                                                paint.style = AndroidPaint.Style.STROKE
                                                                paint.alpha = (obj.opacity * 0.4f * 255).toInt()
                                                                canvas.drawPath(path, paint)
                                                                
                                                                // Solid core white
                                                                paint.color = Color.White.toArgb()
                                                                paint.strokeWidth = obj.strokeWidth * 0.7f * density
                                                                paint.alpha = (obj.opacity * 255).toInt()
                                                                canvas.drawPath(path, paint)
                                                            } else {
                                                                paint.style = AndroidPaint.Style.STROKE
                                                                paint.alpha = (obj.opacity * 255).toInt()
                                                                canvas.drawPath(path, paint)
                                                            }
                                                        }
                                                    }
                                                    ObjectType.TEXT -> {
                                                        paint.style = AndroidPaint.Style.FILL
                                                        paint.textSize = 24f * obj.scale * density
                                                        paint.textAlign = AndroidPaint.Align.CENTER
                                                        paint.setShadowLayer(12f, 0f, 0f, obj.color)
                                                        canvas.save()
                                                        canvas.translate(obj.x, obj.y)
                                                        canvas.rotate(obj.rotation)
                                                        canvas.drawText(obj.text, 0f, 0f, paint)
                                                        canvas.restore()
                                                    }
                                                    ObjectType.IMAGE -> {
                                                        paint.style = AndroidPaint.Style.FILL
                                                        canvas.save()
                                                        canvas.translate(obj.x, obj.y)
                                                        canvas.rotate(obj.rotation)
                                                        if (obj.imageUrl == "ic_sticker_heart") {
                                                            val hrPath = android.graphics.Path().apply {
                                                                moveTo(0f, -15f)
                                                                cubicTo(-25f, -40f, -50f, -10f, 0f, 30f)
                                                                cubicTo(50f, -10f, 25f, -40f, 0f, -15f)
                                                            }
                                                            paint.color = Color(0xFFFF0055).toArgb()
                                                            canvas.drawPath(hrPath, paint)
                                                        } else if (obj.imageUrl == "ic_sticker_star") {
                                                            val starPath = android.graphics.Path()
                                                            val rOut = 30f * obj.scale
                                                            val rIn = 12f * obj.scale
                                                            for (k in 0 until 10) {
                                                                val angle = k * Math.PI / 5
                                                                val rConstraint = if (k % 2 == 0) rOut else rIn
                                                                val px = (rConstraint * sin(angle)).toFloat()
                                                                val py = (-rConstraint * cos(angle)).toFloat()
                                                                if (k == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                                                            }
                                                            starPath.close()
                                                            paint.color = Color(0xFFFFFF00).toArgb()
                                                            canvas.drawPath(starPath, paint)
                                                        } else {
                                                            val bPath = android.graphics.Path().apply {
                                                                moveTo(-10f, -10f)
                                                                lineTo(10f, -10f)
                                                                lineTo(20f, 15f)
                                                                lineTo(-20f, 15f)
                                                                close()
                                                            }
                                                            paint.color = Color(0xFF00FFCC).toArgb()
                                                            canvas.drawPath(bPath, paint)
                                                        }
                                                        canvas.restore()
                                                    }
                                                }
                                            }

                                            // 3. Execute Wallpaper Set with ViewModel
                                            viewModel.applyWorkspaceWallpaper(bitmap, isHome, isLock) { done ->
                                                isApplying = false
                                                applyStatusMessage = if (done) {
                                                    "Success! Synced Wallpaper applied."
                                                } else {
                                                    "Failed setting background."
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.08f)),
                                        modifier = Modifier.weight(1f).testTag("dialog_apply_$text")
                                    ) {
                                        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
