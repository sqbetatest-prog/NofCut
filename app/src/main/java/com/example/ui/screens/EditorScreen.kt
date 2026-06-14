package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TextOverlay
import com.example.data.VideoClip
import com.example.ui.VideoEditorViewModel
import com.example.ui.theme.*
import java.util.UUID
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: VideoEditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clips by viewModel.clips.collectAsStateWithLifecycle()
    val textOverlays by viewModel.textOverlays.collectAsStateWithLifecycle()
    val musicTrack by viewModel.musicTrack.collectAsStateWithLifecycle()
    val currentTimeMs by viewModel.currentTimeMs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val selectedClipId by viewModel.selectedClipId.collectAsStateWithLifecycle()
    val activeRatio by viewModel.activeRatio.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.totalDurationMs.collectAsStateWithLifecycle()
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val exportingFormat by viewModel.exportingFormat.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showAdjustSheet by remember { mutableStateOf(false) }
    var showTextSheet by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }
    var showClipAdderSheet by remember { mutableStateOf(false) }
    var selectedSpeedClipId by remember { mutableStateOf<String?>(null) }
    var showAiPanel by remember { mutableStateOf(false) }
    var showTransitionSheet by remember { mutableStateOf(false) }

    var textToAdd by remember { mutableStateOf("") }
    var textStartSec by remember { mutableStateOf("0") }
    var textDurationSec by remember { mutableStateOf("4") }

    // Derive active editing clip index and offset
    val activeClipData = remember(currentTimeMs, clips) {
        var elapsed = 0L
        var foundClip: VideoClip? = null
        var foundOffset = 0L
        for (cl in clips) {
            val d = (cl.durationMs / cl.speed).toLong()
            if (currentTimeMs >= elapsed && currentTimeMs <= elapsed + d) {
                foundClip = cl
                foundOffset = currentTimeMs - elapsed
                break
            }
            elapsed += d
        }
        if (foundClip == null && clips.isNotEmpty()) {
            foundClip = clips.last()
            foundOffset = (foundClip.durationMs / foundClip.speed).toLong()
        }
        Pair(foundClip, foundOffset)
    }

    val activeClip = activeClipData.first
    val activeClipOffset = activeClipData.second

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentProject?.title ?: "Редактор",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 18.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Сохранить и Выйти")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextActive
                ),
                actions = {
                    // AI Assistant Spark button
                    IconButton(
                        onClick = { showAiPanel = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), CircleShape)
                            .background(DarkSurfaceElevated)
                            .size(36.dp)
                            .testTag("editor_ai_assist_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "ИИ Ассистент",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Ratio selector badge
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .border(1.dp, CustomBorder, RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .clickable {
                                // Toggle ratios
                                val nextRatio = when (activeRatio) {
                                    "9:16" -> "16:9"
                                    "16:9" -> "1:1"
                                    else -> "9:16"
                                }
                                viewModel.updateRatio(nextRatio)
                            }
                            .padding(vertical = 6.dp, horizontal = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (activeRatio) {
                                    "9:16" -> Icons.Rounded.StayCurrentPortrait
                                    "16:9" -> Icons.Rounded.StayCurrentLandscape
                                    else -> Icons.Rounded.CropSquare
                                },
                                contentDescription = "Кадр",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(activeRatio, color = TextActive, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("export_trigger_btn")
                    ) {
                        Icon(imageVector = Icons.Rounded.IosShare, contentDescription = "Экспорт", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Экспорт", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Screen preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .background(DarkBackground)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Device aspect framework wrapper
                val ratioModifier = when (activeRatio) {
                    "9:16" -> Modifier
                        .fillMaxHeight()
                        .aspectRatio(9f / 16f)
                    "16:9" -> Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                    else -> Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                }

                Box(
                    modifier = ratioModifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.dp, CustomBorder, RoundedCornerShape(12.dp))
                ) {
                    // High-fidelity procedural drawing preview canvas!
                    val elapsed = currentTimeMs
                    val filter = activeClip?.filterName ?: "None"
                    val style = activeClip?.styleTheme ?: "Neon"

                    val brightness = activeClip?.brightness ?: 1.0f
                    val contrast = activeClip?.contrast ?: 1.0f
                    val saturation = activeClip?.saturation ?: 1.0f

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2
                        val cy = h / 2

                        // 1. Draw procedural background scenery matching styling
                        when (style) {
                            "Cyberpunk" -> {
                                // Neon city gradient background
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF0E0118), Color(0xFF140D2F))
                                    )
                                )
                                // Scroll grid 3D lines
                                val gridOffset = ((elapsed / 25f) % 40).toInt()
                                val startY = cy - 20f
                                for (y in 0..12) {
                                    val screenY = startY + (y * y * 4f) + gridOffset
                                    if (screenY < h) {
                                        drawLine(
                                            color = Color(0xFF9E00FF).copy(alpha = 0.2f),
                                            start = Offset(0f, screenY),
                                            end = Offset(w, screenY),
                                            strokeWidth = 2f
                                        )
                                    }
                                }
                                // Glowing sun and mountains
                                drawCircle(
                                    color = Color(0xFFFF007F).copy(alpha = 0.62f),
                                    radius = 65.dp.toPx(),
                                    center = Offset(cx, cy - 30.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF00FFCC).copy(alpha = 0.35f),
                                    radius = 35.dp.toPx(),
                                    center = Offset(cx, cy - 30.dp.toPx())
                                )
                            }
                            "Nature" -> {
                                // Sky and forest gradient
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFE0F7FA), Color(0xFF00838F))
                                    )
                                )
                                // Sunset sun
                                drawCircle(
                                    color = Color(0xFFFFA726),
                                    radius = 50.dp.toPx(),
                                    center = Offset(cx + 40.dp.toPx(), cy - 40.dp.toPx())
                                )
                                // Mountains curves
                                val mountainPath = androidx.compose.ui.graphics.Path()
                                mountainPath.moveTo(0f, h)
                                mountainPath.quadraticTo(cx - 100f, cy, cx + 50f, h - 100f)
                                mountainPath.quadraticTo(cx + 200f, cy + 50f, w, h)
                                drawPath(
                                    path = mountainPath,
                                    color = Color(0xFF2E7D32).copy(alpha = 0.85f)
                                )
                            }
                            else -> {
                                // Cozy warmth lamp grid
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF3E2723), Color(0xFF1A0C00))
                                    )
                                )
                                // Cozy lamp aura
                                val waveFactor = (sin(elapsed / 180.0) * 15.0).toFloat()
                                drawCircle(
                                    color = Color(0xFFFFCC80).copy(alpha = 0.45f),
                                    radius = 80.dp.toPx() + waveFactor,
                                    center = Offset(cx, cy - 25.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFFFFB74D).copy(alpha = 0.15f),
                                    radius = 160.dp.toPx() + waveFactor * 0.5f,
                                    center = Offset(cx, cy - 25.dp.toPx())
                                )
                            }
                        }

                        // 2. Compute Filter Effects (Noir/Greyscale, Vaporwave / Purple boost, Chrome aberration)
                        when (filter) {
                            "Noir" -> {
                                // Abstract overlay for Noir
                                drawRect(
                                    color = Color.White.copy(alpha = 0.05f),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.Color
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.22f),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.Saturation
                                )
                            }
                            "Vaporwave" -> {
                                drawRect(
                                    color = Color(0xFFFF00FF).copy(alpha = 0.18f),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.Hue
                                )
                            }
                            "Cyberpunk" -> {
                                drawRect(
                                    color = Color(0xFF00FFF0).copy(alpha = 0.12f),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.Overlay
                                )
                            }
                            "Vintage" -> {
                                drawRect(
                                    color = Color(0xFF8B5A2B).copy(alpha = 0.15f),
                                    blendMode = androidx.compose.ui.graphics.BlendMode.Color
                                )
                            }
                            else -> {}
                        }

                        // 3. Brightness & Contrast correction simulation drawing overlay
                        if (brightness != 1.0f) {
                            drawRect(
                                color = if (brightness > 1.0f) Color.White.copy(alpha = (brightness - 1.0f).coerceIn(0f, 0.5f))
                                else Color.Black.copy(alpha = (1.0f - brightness).coerceIn(0f, 0.8f))
                            )
                        }

                        // Playback timeline sync lines (mini tracker inside preview)
                        drawRect(
                            color = Color.White.copy(alpha = 0.03f),
                            size = size,
                            style = Stroke(width = 0.8.dp.toPx())
                        )
                    }

                    // 4. Subtitle overlay rendering overlays (centered / bottom relative)
                    textOverlays.forEach { item ->
                        if (elapsed >= item.startTimeMs && elapsed <= item.endTimeMs) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 32.dp, horizontal = 20.dp),
                                contentAlignment = when (item.align) {
                                    "Top" -> Alignment.TopCenter
                                    "Bottom" -> Alignment.BottomCenter
                                    else -> Alignment.Center
                                }
                            ) {
                                Text(
                                    text = item.text,
                                    color = Color(android.graphics.Color.parseColor(item.colorHex)),
                                    fontSize = item.fontSize.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    style = LocalTextStyle.current.copy(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color.Black.copy(alpha = 0.95f),
                                            offset = Offset(2f, 2f),
                                            blurRadius = 6f
                                        )
                                    ),
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Transitions rendering (drawn dynamically between clips borders)
                    var borderAccum = 0L
                    clips.forEachIndexed { i, item ->
                        val duration = (item.durationMs / item.speed).toLong()
                        val transitionType = item.transition
                        
                        // We are near the split/border between item `i` and `i+1` (which occurs at `borderAccum + duration`)
                        val boundaryTime = borderAccum + duration
                        val diff = elapsed - boundaryTime // negative means before split, positive means after split
                        val windowMs = 300L // 300ms transition duration
                        
                        if (kotlin.math.abs(diff) < windowMs && i < clips.size - 1) {
                            val progress = (diff + windowMs).toFloat() / (2 * windowMs) // 0f to 1f
                            
                            when (transitionType) {
                                "Вспышка" -> {
                                    // A peak in the center: progress = 0.5 -> alpha = 1.0f
                                    val alpha = 1.0f - (kotlin.math.abs(progress - 0.5f) * 2f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White.copy(alpha = alpha.coerceIn(0f, 1f)))
                                    )
                                }
                                "Затухание" -> {
                                    // Black fade peaking in the center
                                    val alpha = 1.0f - (kotlin.math.abs(progress - 0.5f) * 2f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = alpha.coerceIn(0f, 1f)))
                                    )
                                }
                                "Глитч" -> {
                                    // Randomized chromatic glitches
                                    val r = (sin(elapsed / 10.0) * 0.5f + 0.5f)
                                    if (r > 0.3f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    if (r > 0.7f) Color(0xFFFF007F).copy(alpha = 0.4f)
                                                    else Color(0xFF00FFFF).copy(alpha = 0.4f)
                                                )
                                        )
                                    }
                                }
                                "Сдвиг" -> {
                                    // Sweep wipe line
                                    val posX = (progress * 400).dp
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(20.dp)
                                            .offset(x = posX)
                                            .background(PrimaryNeon)
                                    )
                                }
                                else -> {
                                    // Default minimal glitch white flash
                                    val alpha = 1.0f - (kotlin.math.abs(progress - 0.5f) * 2f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White.copy(alpha = (alpha * 0.2f).coerceIn(0f, 1f)))
                                    )
                                }
                            }
                        }
                        borderAccum += duration
                    }
                }
            }

            // Player progress scrub metrics & controls
            val elapsedSecs = remember(currentTimeMs) { currentTimeMs / 1000f }
            val totalSecs = remember(totalDurationMs) { totalDurationMs / 1000f }
            val progressFactor = if (totalDurationMs > 0) currentTimeMs.toFloat() / totalDurationMs else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(vertical = 6.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time coordinates row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%.2fс", elapsedSecs),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Black
                    )

                    // Compact mini quick play buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.seekTo((currentTimeMs - 2000).coerceAtLeast(0L)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Replay10, contentDescription = "-2 сек", modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = { viewModel.togglePlayback() },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryNeon),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("play_pause_fab")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Пауза" else "Играть",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.seekTo((currentTimeMs + 2000).coerceAtMost(totalDurationMs)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Forward10, contentDescription = "+2 сек", modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        text = String.format("%.2fs", totalSecs),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSubtle
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom Slider scrubbing zone (can tap or drag precisely to seek)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(DarkBoard)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val positionFraction = offset.x / size.width
                                val newHead = (positionFraction * totalDurationMs).toLong()
                                viewModel.seekTo(newHead)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFactor.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(PrimaryNeon)
                    )
                }
            }

            // Timeline Tracks container with Drag / Zoom / Scrub zones
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .background(DarkBackground)
            ) {
                // Track 1. Visual Playback Time Ruler
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(DarkSurfaceElevated)
                        .border(BorderStroke(0.5.dp, CustomBorder))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val fractionDelta = dragAmount.x / size.width
                                val timeDelta = (fractionDelta * totalDurationMs).toLong()
                                viewModel.seekTo((currentTimeMs + timeDelta).coerceIn(0L, totalDurationMs))
                            }
                        }
                ) {
                    // Tick indicators
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val rulerW = size.width
                        val numTicks = 11
                        for (i in 0 until numTicks) {
                            val x = (i.toFloat() / (numTicks - 1)) * rulerW
                            val isLabel = i % 2 == 0
                            val tickH = if (isLabel) 12.dp.toPx() else 6.dp.toPx()

                            drawLine(
                                color = TextSubtle.copy(alpha = 0.5f),
                                start = Offset(x, 0f),
                                end = Offset(x, tickH),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // Cursor indicator head
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.5.dp)
                            .align(Alignment.CenterStart)
                            .offset(x = (progressFactor * 360).dp) // simple linear representation
                            .background(Color.Red)
                    )
                }

                // Scrollable container for tracks
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp, horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Track 2: Clips Sequence Track Row
                    Column {
                        Text(
                            text = "ВИДЕОКАДРЫ (Нажмите для выбора)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Horizontal clip blocks
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(1.dp, CustomBorder, RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (clips.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Нет добавленных кадров", color = TextSubtle, fontSize = 11.sp)
                                }
                            } else {
                                clips.forEachIndexed { index, clip ->
                                    val isClipSelected = selectedClipId == clip.id
                                    val durationFact = if (totalDurationMs > 0) clip.durationMs.toFloat() / totalDurationMs else 0.3f
                                    val weightFactor = (durationFact * 10f).coerceAtLeast(1f)

                                    val bgCol = remember(clip.colorHex) {
                                        try { Color(android.graphics.Color.parseColor(clip.colorHex)) } catch (e: Exception) { Color.Gray }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(weightFactor)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(bgCol.copy(alpha = if (isClipSelected) 0.95f else 0.45f))
                                            .border(
                                                width = if (isClipSelected) 2.dp else 0.5.dp,
                                                color = if (isClipSelected) Color.White else CustomBorder,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { viewModel.selectClip(clip.id) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                text = clip.title,
                                                color = if (isClipSelected) Color.Black else TextActive,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Text(
                                                    text = "${clip.durationMs / 1000}s",
                                                    color = if (isClipSelected) Color.Black.copy(alpha = 0.7f) else TextSubtle,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (clip.speed != 1.0f) {
                                                    Text(
                                                        text = "${clip.speed}x",
                                                        color = if (isClipSelected) Color.Black else PrimaryNeon,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Track 3: Subtitles Track
                    Column {
                        Text(
                            text = "СУБТИТРЫ / ТЕКСТ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .border(1.dp, CustomBorder, RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (textOverlays.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { showTextSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+ Добавить текст на дорожку", color = TextSubtle, fontSize = 10.sp)
                                }
                            } else {
                                textOverlays.forEach { txt ->
                                    val isCurrentlyRendering = currentTimeMs >= txt.startTimeMs && currentTimeMs <= txt.endTimeMs
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = if (isCurrentlyRendering) 1.5.dp else 0.5.dp,
                                                color = if (isCurrentlyRendering) PrimaryNeon else CustomBorder,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .background(if (isCurrentlyRendering) PrimaryNeon.copy(alpha = 0.15f) else DarkSurfaceElevated)
                                            .padding(horizontal = 8.dp)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "\"${txt.text}\"",
                                                fontSize = 9.sp,
                                                color = TextActive,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                modifier = Modifier.widthIn(max = 100.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.Cancel,
                                                contentDescription = "Удалить",
                                                tint = TextSubtle,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable { viewModel.removeTextOverlay(txt.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Track 4: Soundtrack Music Track Custom Blocks
                    Column {
                        Text(
                            text = "МУЗЫКА ТЕМЫ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (musicTrack != null) Color(0xFF1B2E3C) else DarkSurface)
                                .border(1.dp, if (musicTrack != null) AccentBlue else CustomBorder, RoundedCornerShape(6.dp))
                                .clickable { showMusicSheet = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = if (musicTrack != null) AccentBlue else TextSubtle,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = musicTrack?.name ?: "Без аудиозаписи (Нажмите для выбора)",
                                        fontSize = 10.sp,
                                        color = if (musicTrack != null) TextActive else TextSubtle,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (musicTrack != null) {
                                    Text(
                                        text = "${(musicTrack!!.volume * 100).toInt()}% громкость",
                                        fontSize = 9.sp,
                                        color = AccentBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // High Fidelity Floating Toolbar Dock for creative quick commands!
            Surface(
                color = DarkSurface,
                border = BorderStroke(1.dp, CustomBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. ADD NEW STOCK FOOTAGE presets
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { showClipAdderSheet = true }
                            .testTag("tool_btn_add_clip")
                    ) {
                        Icon(imageVector = Icons.Rounded.VideoCall, contentDescription = "+ Кадр", tint = PrimaryNeon)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("+ Клип", style = MaterialTheme.typography.labelSmall, color = TextActive)
                    }

                    // 2. SPLIT CUT action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { viewModel.splitSelectedClip() }
                            .testTag("tool_btn_split")
                    ) {
                        Icon(imageVector = Icons.Rounded.ContentCut, contentDescription = "Разделить", tint = TextActive)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Сплит", style = MaterialTheme.typography.labelSmall, color = TextActive)
                    }

                    // 3. COLOR FILTER Preset shaders
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { showFilterSheet = true }
                            .testTag("tool_btn_filter")
                    ) {
                        Icon(imageVector = Icons.Rounded.FilterBAndW, contentDescription = "Фильтры", tint = TextActive)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Фильтр", style = MaterialTheme.typography.labelSmall, color = TextActive)
                    }

                    // 4. AUDIO TRACK Selector Dialog
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showMusicSheet = true }
                    ) {
                        Icon(imageVector = Icons.Rounded.LibraryMusic, contentDescription = "Музыка", tint = TextActive)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Музыка", style = MaterialTheme.typography.labelSmall, color = TextActive)
                    }

                    // 5. SUBTITLE Overlay drawer
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showTextSheet = true }
                    ) {
                        Icon(imageVector = Icons.Rounded.TextFields, contentDescription = "Текст", tint = TextActive)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Текст", style = MaterialTheme.typography.labelSmall, color = TextActive)
                    }

                    // 6. SLIDERS: Saturation, Contrast, Brightness adjustments
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showAdjustSheet = true }
                    ) {
                        Icon(imageVector = Icons.Rounded.Tune, contentDescription = "Параметры", tint = TextActive)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Адаптация", style = MaterialTheme.typography.labelSmall, color = TextActive)
                    }

                    // 7. SPEED multiplier badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedSpeedClipId = selectedClipId }
                    ) {
                        Icon(imageVector = Icons.Rounded.Speed, contentDescription = "Скорость", tint = TextActive)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Скорость", style = MaterialTheme.typography.labelSmall, color = TextActive)
                    }

                    // 7.5 TRANSITIONS selector
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { showTransitionSheet = true }
                            .testTag("tool_btn_transitions")
                    ) {
                        val activeClipTransition = activeClip?.transition ?: "Нет"
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = "Переход",
                            tint = if (activeClipTransition != "Нет") PrimaryNeon else TextActive
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (activeClipTransition != "Нет") activeClipTransition else "Переход",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activeClipTransition != "Нет") PrimaryNeon else TextActive
                        )
                    }

                    // 8. DELETE active clip block
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.deleteSelectedClip() }
                    ) {
                        Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = "Удалить клип", tint = Color(0xFFFF4F4F))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Удалить", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF4F4F))
                    }
                }
            }
        }

        // Export Spinner HUD dialog
        if (exportProgress != null) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = DarkSurfaceElevated,
                title = {
                    Text(
                        "Рендеринг видео...",
                        fontWeight = FontWeight.Bold,
                        color = TextActive
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { (exportProgress ?: 0) / 100f },
                            color = PrimaryNeon,
                            trackColor = CustomBorder,
                            strokeWidth = 5.dp,
                            modifier = Modifier.size(76.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Прогресс: $exportProgress%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryNeon
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Кодирование слоев в формат $exportingFormat. Пожалуйста, не закрывайте студию.",
                            fontSize = 11.sp,
                            color = TextSubtle,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {}
            )
        }

        // Format Export Menu Configuration
        if (showExportDialog) {
            var selectedFormat by remember { mutableStateOf("1080p | 60 FPS") }

            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                containerColor = DarkSurfaceElevated,
                title = {
                    Text(
                        "Настройки рендеринга",
                        fontWeight = FontWeight.Bold,
                        color = TextActive
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Выберите качество экспортируемого файла. Все аудиодорожки, фильтры и наложения субтитров будут объединены.",
                            fontSize = 11.sp,
                            color = TextSubtle
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        listOf("4K UHD | 30 FPS", "1080p | 60 FPS", "1080p | 30 FPS", "720p HD | 30 FPS").forEach { fmt ->
                            val isSelected = selectedFormat == fmt
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryNeon.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { selectedFormat = fmt }
                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fmt,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PrimaryNeon else TextActive
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedFormat = fmt },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryNeon, unselectedColor = CustomBorder)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExportDialog = false
                            viewModel.startExportSimulation(selectedFormat) {
                                onBack() // Go back to projects or gallery to preview it!
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = CircleShape
                    ) {
                        Text("Начать рендер", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = TextSubtle)) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Dynamic Filters preset Sheet
        if (showFilterSheet) {
            AlertDialog(
                onDismissRequest = { showFilterSheet = false },
                containerColor = DarkSurfaceElevated,
                title = { Text("Выберите цветовой LUT/Фильтр", fontWeight = FontWeight.Bold, color = TextActive) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Triple("None", "Оригинал", "Стандартная цветовая гамма"),
                            Triple("Noir", "Глубокий Noir", "Высококонтрастный ч/б кинематограф"),
                            Triple("Vaporwave", "Vaporwave Preset", "Клубные фиолетовые тона"),
                            Triple("Cyberpunk", "Neon Cyberpunk", "Яркие бирюзовые и розовые акценты"),
                            Triple("Vintage", "Classic Vintage", "Теплый сепия и пленочный шум")
                        ).forEach { (filterKey, filterHeader, filterDesc) ->
                            val isActiveFilter = activeClip?.filterName == filterKey
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActiveFilter) PrimaryNeon.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        viewModel.updateSelectedClip { it.copy(filterName = filterKey) }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = filterHeader, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isActiveFilter) PrimaryNeon else TextActive)
                                    Text(text = filterDesc, fontSize = 10.sp, color = TextSubtle)
                                }
                                if (isActiveFilter) {
                                    Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = "Active", tint = PrimaryNeon)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showFilterSheet = false }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color.Black)) {
                        Text("Готово", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Clip Speed Change popup
        if (selectedSpeedClipId != null) {
            AlertDialog(
                onDismissRequest = { selectedSpeedClipId = null },
                containerColor = DarkSurfaceElevated,
                title = { Text("Множитель скорости", fontWeight = FontWeight.Bold, color = TextActive) },
                text = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.5f, 1.0f, 1.5f, 2.0f, 4.0f).forEach { spd ->
                            val isCurrentSpeed = activeClip?.speed == spd
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrentSpeed) PrimaryNeon else CustomBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(if (isCurrentSpeed) PrimaryNeon.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        viewModel.updateSelectedClip { it.copy(speed = spd) }
                                        selectedSpeedClipId = null
                                    }
                                    .padding(vertical = 12.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${spd}x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentSpeed) PrimaryNeon else TextActive
                                )
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // Text subtitle inserter sheet
        if (showTextSheet) {
            AlertDialog(
                onDismissRequest = { showTextSheet = false },
                containerColor = DarkSurfaceElevated,
                title = { Text("Добавить наложение текста", fontWeight = FontWeight.Bold, color = TextActive) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = textToAdd,
                            onValueChange = { textToAdd = it },
                            label = { Text("Текст субтитра") },
                            placeholder = { Text("Введите слова...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryNeon,
                                unfocusedBorderColor = CustomBorder,
                                focusedTextColor = TextActive,
                                unfocusedTextColor = TextActive
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subtitle_text_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = textStartSec,
                                onValueChange = { textStartSec = it },
                                label = { Text("Старт (сек)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryNeon,
                                    unfocusedBorderColor = CustomBorder,
                                    focusedTextColor = TextActive,
                                    unfocusedTextColor = TextActive
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = textDurationSec,
                                onValueChange = { textDurationSec = it },
                                label = { Text("Длительность (сек)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryNeon,
                                    unfocusedBorderColor = CustomBorder,
                                    focusedTextColor = TextActive,
                                    unfocusedTextColor = TextActive
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val promptText = textToAdd.ifEmpty { "НОВЫЙ ТЕКСТ" }
                            val startFloat = textStartSec.toFloatOrNull() ?: 0f
                            val durationFloat = textDurationSec.toFloatOrNull() ?: 4f
                            viewModel.addTextOverlay(promptText, startFloat, durationFloat)
                            textToAdd = ""
                            showTextSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color.Black)
                    ) {
                        Text("Добавить", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextSheet = false }, colors = ButtonDefaults.textButtonColors(contentColor = TextSubtle)) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Visual adjust sliders dialog (Smart Color Correction)
        if (showAdjustSheet) {
            var inputBrightness by remember { mutableStateOf(activeClip?.brightness ?: 1.0f) }
            var inputContrast by remember { mutableStateOf(activeClip?.contrast ?: 1.0f) }
            var inputSaturation by remember { mutableStateOf(activeClip?.saturation ?: 1.0f) }
            var currentPresetName by remember { mutableStateOf("Manual") }
            var activeTintHex by remember { mutableStateOf(activeClip?.colorHex ?: "#FF2D55") }
            var applyGlobally by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAdjustSheet = false },
                containerColor = DarkSurfaceElevated,
                title = { Text("Умная цветокоррекция", fontWeight = FontWeight.Bold, color = TextActive, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Presets Header
                        Text("БЫСТРЫЕ ФИЛЬТРЫ-ПРЕСЕТЫ:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextSubtle, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("Яркий", 1.3f, Pair(1.2f, 1.4f)),
                                Triple("Киношный", 0.85f, Pair(1.35f, 0.8f)),
                                Triple("Черно-белый", 1.0f, Pair(1.5f, 0.0f))
                            ).forEach { (presetLabel, bVal, pairVal) ->
                                val (cVal, sVal) = pairVal
                                val isPresetSelected = currentPresetName == presetLabel
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isPresetSelected) PrimaryNeon else DarkSurface,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isPresetSelected) PrimaryNeon else CustomBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            currentPresetName = presetLabel
                                            inputBrightness = bVal
                                            inputContrast = cVal
                                            inputSaturation = sVal
                                            activeTintHex = when (presetLabel) {
                                                "Яркий" -> "#00FF66"
                                                "Киношный" -> "#00A896"
                                                "Черно-белый" -> "#777777"
                                                else -> "#FF2D55"
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = presetLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPresetSelected) Color.Black else TextActive
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Manual Tune sliders
                        Text("РУЧНЫЕ НАСТРОЙКИ КОРРЕКЦИИ:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextSubtle, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Brightness
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Яркость", fontSize = 11.sp, color = TextActive)
                            Text(text = String.format("%.2f", inputBrightness), fontSize = 11.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = inputBrightness,
                            onValueChange = {
                                inputBrightness = it
                                currentPresetName = "Вручную"
                            },
                            valueRange = 0.3f..1.8f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryNeon, activeTrackColor = PrimaryNeon)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Contrast
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Контраст", fontSize = 11.sp, color = TextActive)
                            Text(text = String.format("%.2f", inputContrast), fontSize = 11.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = inputContrast,
                            onValueChange = {
                                inputContrast = it
                                currentPresetName = "Вручную"
                            },
                            valueRange = 0.3f..1.8f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryNeon, activeTrackColor = PrimaryNeon)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Saturation
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Насыщенность", fontSize = 11.sp, color = TextActive)
                            Text(text = String.format("%.2f", inputSaturation), fontSize = 11.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = inputSaturation,
                            onValueChange = {
                                inputSaturation = it
                                currentPresetName = "Вручную"
                            },
                            valueRange = 0.0f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryNeon, activeTrackColor = PrimaryNeon)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Tint Palette (оттенок)
                        Text("ЦВЕТОВОЙ ОТТЕНОК (TINT):", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextSubtle, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                Pair("#FF2D55", "Неон"),
                                Pair("#00F0FF", "Циан"),
                                Pair("#2E8B57", "Лес"),
                                Pair("#FFA500", "Закат"),
                                Pair("#777777", "Серый")
                            ).forEach { (colorHexVal, nameVal) ->
                                val isSelectedColor = activeTintHex.lowercase() == colorHexVal.lowercase()
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHexVal)))
                                        .border(
                                            width = if (isSelectedColor) 3.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            activeTintHex = colorHexVal
                                            currentPresetName = "Вручную"
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scope choice: Single clip or Global Apply
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .clickable { applyGlobally = !applyGlobally }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Применить ко всем клипам", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextActive)
                                Text("Изменение затронет всю монтажную ленту проекта", fontSize = 10.sp, color = TextSubtle)
                            }
                            Switch(
                                checked = applyGlobally,
                                onCheckedChange = { applyGlobally = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryNeon, checkedTrackColor = PrimaryNeon.copy(alpha = 0.4f))
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (applyGlobally) {
                                viewModel.applyColorCorrectionToAllClips(
                                    filterName = currentPresetName,
                                    brightness = inputBrightness,
                                    contrast = inputContrast,
                                    saturation = inputSaturation,
                                    colorHex = activeTintHex
                                )
                            } else {
                                viewModel.updateSelectedClip { clip ->
                                    clip.copy(
                                        filterName = currentPresetName,
                                        brightness = inputBrightness,
                                        contrast = inputContrast,
                                        saturation = inputSaturation,
                                        colorHex = activeTintHex
                                    )
                                }
                            }
                            showAdjustSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color.Black)
                    ) {
                        Text("Применить", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdjustSheet = false }, colors = ButtonDefaults.textButtonColors(contentColor = TextSubtle)) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Music Soundtrack chooser Sheet
        if (showMusicSheet) {
            AlertDialog(
                onDismissRequest = { showMusicSheet = false },
                containerColor = DarkSurfaceElevated,
                title = { Text("Музыкальное сопровождение", fontWeight = FontWeight.Bold, color = TextActive) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Pair("Без музыки", "Выключить фоновые beats"),
                            Pair("Synthwave Speedtrack", "Электронная динамика в стиле Cyber"),
                            Pair("Guitar Forest Trail", "Спокойное акустическое сопровождение"),
                            Pair("Lofi Coffee Sip", "Расслабленный Lo-fi джаз ритм")
                        ).forEach { (trackName, desc) ->
                            val isSelected = (musicTrack == null && trackName == "Без музыки") || (musicTrack?.name == trackName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentBlue.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        viewModel.updateMusicTrack(trackName)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = trackName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) AccentBlue else TextActive
                                    )
                                    Text(text = desc, fontSize = 10.sp, color = TextSubtle)
                                }
                                if (isSelected) {
                                    Icon(imageVector = Icons.Rounded.VolumeUp, contentDescription = "Active", tint = AccentBlue)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showMusicSheet = false }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White)) {
                        Text("Готово", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Add pre-configured stock footage item (Stock Media Library Dialog)
        if (showClipAdderSheet) {
            var selectedStockTab by remember { mutableStateOf("video") }
            var stockSearchQuery by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showClipAdderSheet = false },
                containerColor = DarkSurfaceElevated,
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Стоковая библиотека медиа", fontWeight = FontWeight.Bold, color = TextActive, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        // Category selector tabs (Vídeo, Image, Audio)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("video", "Видео", Icons.Rounded.Movie),
                                Triple("image", "Фото", Icons.Rounded.Image),
                                Triple("music", "Музыка", Icons.Rounded.MusicNote)
                            ).forEach { (tabKey, tabLabel, icon) ->
                                val isActive = selectedStockTab == tabKey
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isActive) PrimaryNeon else DarkSurface,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isActive) PrimaryNeon else CustomBorder,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { selectedStockTab = tabKey }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isActive) Color.Black else TextSubtle,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tabLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color.Black else TextActive
                                    )
                                }
                            }
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        // Search field input under tabs
                        OutlinedTextField(
                            value = stockSearchQuery,
                            onValueChange = { stockSearchQuery = it },
                            placeholder = { Text("Поиск стоковых файлов...", fontSize = 12.sp, color = TextSubtle) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(16.dp))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryNeon,
                                unfocusedBorderColor = CustomBorder,
                                focusedTextColor = TextActive,
                                unfocusedTextColor = TextActive
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedStockTab == "video") {
                                val videos = listOf(
                                    Triple("Неоновый Перекресток", 6000L, Pair("Cyberpunk", "#FF007F")),
                                    Triple("Капли Дождя на стекле", 5000L, Pair("Cyberpunk", "#00FFF0")),
                                    Triple("Лесной Ручей в тумане", 7000L, Pair("Nature", "#2E8B57")),
                                    Triple("Закат на перевале", 6000L, Pair("Nature", "#FFA500")),
                                    Triple("Книжный шкаф при свечах", 8000L, Pair("Cozy", "#CD853F")),
                                    Triple("Мягкая Кофемашина", 6000L, Pair("Cozy", "#8B4513"))
                                ).filter { it.first.contains(stockSearchQuery, ignoreCase = true) }

                                if (videos.isEmpty()) {
                                    Text("Ничего не найдено", color = TextSubtle, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                                } else {
                                    videos.forEach { (caption, dur, tuple) ->
                                        val (styleTheme, colorHex) = tuple
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                            border = BorderStroke(1.dp, CustomBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val generatedPresetClip = VideoClip(
                                                        id = UUID.randomUUID().toString(),
                                                        title = caption,
                                                        durationMs = dur,
                                                        styleTheme = styleTheme,
                                                        colorHex = colorHex
                                                    )
                                                    viewModel.addClip(generatedPresetClip)
                                                    showClipAdderSheet = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val indicatorColor = remember(colorHex) { Color(android.graphics.Color.parseColor(colorHex)) }
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(indicatorColor)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(text = caption, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextActive)
                                                    Text(text = "Видео • $styleTheme • ${dur / 1000}с", fontSize = 10.sp, color = TextSubtle)
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(imageVector = Icons.Rounded.Download, contentDescription = "Добавить", tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            } else if (selectedStockTab == "image") {
                                val images = listOf(
                                    Triple("Ретро Фотография", 5000L, Pair("Retro", "#E5D3B3")),
                                    Triple("Акварельный Рисунок", 4000L, Pair("Aesthetic", "#FFC0CB")),
                                    Triple("Футуристичный Постер", 6000L, Pair("Neon", "#7F00FF")),
                                    Triple("Городская Архитектура", 5000L, Pair("Modern", "#D3D3D3")),
                                    Triple("Осенний Пейзаж", 7000L, Pair("Nature", "#D2691E"))
                                ).filter { it.first.contains(stockSearchQuery, ignoreCase = true) }

                                if (images.isEmpty()) {
                                    Text("Ничего не найдено", color = TextSubtle, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                                } else {
                                    images.forEach { (caption, dur, tuple) ->
                                        val (styleTheme, colorHex) = tuple
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                            border = BorderStroke(1.dp, CustomBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val generatedPresetClip = VideoClip(
                                                        id = UUID.randomUUID().toString(),
                                                        title = caption,
                                                        durationMs = dur,
                                                        styleTheme = styleTheme,
                                                        colorHex = colorHex
                                                    )
                                                    viewModel.addClip(generatedPresetClip)
                                                    showClipAdderSheet = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val indicatorColor = remember(colorHex) { Color(android.graphics.Color.parseColor(colorHex)) }
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(indicatorColor)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(text = caption, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextActive)
                                                    Text(text = "Изображение • $styleTheme • ${dur / 1000}с", fontSize = 10.sp, color = TextSubtle)
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(imageVector = Icons.Rounded.Download, contentDescription = "Добавить", tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                val audios = listOf(
                                    Pair("Synthwave Speedtrack", "Электронная динамика в стиле Cyber"),
                                    Pair("Guitar Forest Trail", "Спокойное акустическое сопровождение"),
                                    Pair("Lofi Coffee Sip", "Расслабленный Lo-fi джаз ритм"),
                                    Pair("Neon Cyberbeat", "Ритмичный синтезаторный бит для видеоблогов"),
                                    Pair("Sunny Acoustic Walk", "Жизнерадостная гитара для путешествий")
                                ).filter { it.first.contains(stockSearchQuery, ignoreCase = true) }

                                if (audios.isEmpty()) {
                                    Text("Ничего не найдено", color = TextSubtle, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                                } else {
                                    audios.forEach { (trackName, desc) ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                            border = BorderStroke(1.dp, CustomBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.updateMusicTrack(trackName)
                                                    showClipAdderSheet = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Rounded.MusicNote, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(text = trackName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextActive)
                                                    Text(text = "Музыка • $desc", fontSize = 10.sp, color = TextSubtle)
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(imageVector = Icons.Rounded.Download, contentDescription = "Добавить", tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showClipAdderSheet = false }, colors = ButtonDefaults.textButtonColors(contentColor = PrimaryNeon)) {
                        Text("Закрыть")
                    }
                }
            )
        }

        // AI Assistant Overlay HUD Dialog
        if (showAiPanel) {
            var isAnalyzing by remember { mutableStateOf(true) }

            // Simulated real-time intelligence timing
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1300)
                isAnalyzing = false
            }

            AlertDialog(
                onDismissRequest = { showAiPanel = false },
                containerColor = DarkSurfaceElevated,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Умный ИИ-Помощник", fontWeight = FontWeight.Bold, color = TextActive)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (isAnalyzing) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = PrimaryNeon, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("ИИ анализирует кадры проекта...", fontSize = 12.sp, color = TextActive)
                                Text("Поиск лучших моментов и битов...", fontSize = 10.sp, color = TextSubtle)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(PrimaryNeon.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        "Анализ завершен! Рекомендуем оптимизировать ваши видеоклипы под social сети.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PrimaryNeon
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Text("ПРЕДЛОЖЕННЫЕ ИИ УЛУЧШЕНИЯ:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextSubtle, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Suggestion 1: Trim clip
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(imageVector = Icons.Rounded.ContentCut, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Умная нарезка фрагментов (Trimming)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextActive)
                                        Text("Избавит от скучных статичных пауз, сократив длительность клипов на 25% для высокой динамики.", fontSize = 10.sp, color = TextSubtle)
                                    }
                                }

                                // Suggestion 2: Auto add background beats matching clip style
                                val recommendedMusic = if (clips.any { it.styleTheme == "Cyberpunk" }) "Synthwave Speedtrack" else "Guitar Forest Trail"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(imageVector = Icons.Rounded.MusicNote, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Автоматическая фоновая музыка", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextActive)
                                        Text("Интегрирует подходящую аудиодорожку «$recommendedMusic» из стоковой библиотеки, выровненную по фазе.", fontSize = 10.sp, color = TextSubtle)
                                    }
                                }

                                // Suggestion 3: Standard Transitions
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(imageVector = Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Стандартные переходы (Glitch Transitions)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextActive)
                                        Text("Автоматически разместит плавные переходы 'Glitch Flash' на стыках монтажного кадра.", fontSize = 10.sp, color = TextSubtle)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (!isAnalyzing) {
                        Button(
                            onClick = {
                                val recommendedMusic = if (clips.any { it.styleTheme == "Cyberpunk" }) "Synthwave Speedtrack" else "Guitar Forest Trail"
                                viewModel.applyAiSuggestions(recommendedMusic, shouldCutClips = true)
                                showAiPanel = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color.Black)
                        ) {
                            Text("Применить все", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAiPanel = false }, colors = ButtonDefaults.textButtonColors(contentColor = TextSubtle)) {
                        Text(if (isAnalyzing) "Отмена" else "Отклонить")
                    }
                }
            )
        }

        // Transition selection sheet dialog
        if (showTransitionSheet) {
            AlertDialog(
                onDismissRequest = { showTransitionSheet = false },
                containerColor = DarkSurfaceElevated,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Bolt, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Кинематографичные переходы", fontWeight = FontWeight.Bold, color = TextActive)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Выберите визуальный эффект для перехода после текущего клика:", fontSize = 11.sp, color = TextSubtle)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        listOf(
                            Triple("Нет", "Без перехода", "Обычная смена кадра встык"),
                            Triple("Вспышка", "Яркая Вспышка (White Flash)", "Импульсивный световой переход на стыке кадров"),
                            Triple("Затухание", "Затемнение (Fade Out)", "Плавный уход в черное и проявление следующего кадра"),
                            Triple("Глитч", "Цифровой Глитч", "Киберпанк помехи, смешивающие два кадра"),
                            Triple("Сдвиг", "Боковой Сдвиг (Slide Swipe)", "Динамичный горизонтальный сдвиг кадра слева направо")
                        ).forEach { (typeVal, labelVal, descVal) ->
                            val isSelected = (activeClip?.transition ?: "Нет") == typeVal
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryNeon.copy(alpha = 0.12f) else DarkSurface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryNeon else CustomBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateSelectedClip { clip ->
                                            clip.copy(transition = typeVal)
                                        }
                                        showTransitionSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) PrimaryNeon else DarkSurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (typeVal) {
                                                "Вспышка" -> Icons.Rounded.LightMode
                                                "Затухание" -> Icons.Rounded.DarkMode
                                                "Глитч" -> Icons.Rounded.FlashOn
                                                "Сдвиг" -> Icons.Rounded.CompareArrows
                                                else -> Icons.Rounded.Block
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) Color.Black else TextSubtle,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = labelVal, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) PrimaryNeon else TextActive)
                                        Text(text = descVal, fontSize = 10.sp, color = TextSubtle)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTransitionSheet = false }, colors = ButtonDefaults.textButtonColors(contentColor = PrimaryNeon)) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}
