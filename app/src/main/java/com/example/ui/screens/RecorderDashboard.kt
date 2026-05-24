package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.RecordingEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.RecorderViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecorderDashboard(
    modifier: Modifier = Modifier,
    viewModel: RecorderViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Permission state
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasMicPermission = isGranted
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (!hasMicPermission) {
            SecurityClearanceGate(onAuthenticate = {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            })
        } else {
            DashboardContent(viewModel = viewModel)
        }
    }
}

@Composable
fun SecurityClearanceGate(onAuthenticate: () -> Unit) {
    // Holographic background grids and pulsar colors
    val infiniteTransition = rememberInfiniteTransition(label = "Security Gate")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha Pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Futuristic shield lock asset
        Box(
            modifier = Modifier
                .size(120.dp)
                .drawBehind {
                    drawCircle(
                        color = JarvisPrimaryNeon,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = JarvisPrimaryNeon.copy(alpha = alphaPulse * 0.15f),
                        radius = (size.minDimension / 2) - 8.dp.toPx()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Security Access Required",
                tint = JarvisPrimaryNeon,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "STARK INDUSTRIES GATEWAY",
            color = JarvisPrimaryNeon,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "BIOMETRIC AUTHENTICATION CODES REQUIRED",
            color = JarvisTertiaryOrange,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CALIBRATION STATUS: LOCKED",
                    color = JarvisOnBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This vocal diagnostic terminal requires access to the system local sound microphone node (RECORD_AUDIO). Proceed with authorization configuration to unlock real-time recording, noise filtering, and telemetry logging.",
                    color = JarvisOnSurface,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAuthenticate,
            colors = ButtonDefaults.buttonColors(containerColor = JarvisPrimaryNeon),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
                .testTag("authenticate_button")
        ) {
            Text(
                text = "AUTHENTICATE MIC NODE",
                color = JarvisBackground,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun DashboardContent(viewModel: RecorderViewModel) {
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentTranscription by viewModel.currentTranscription.collectAsStateWithLifecycle()
    val liveAmplitude by viewModel.liveAmplitude.collectAsStateWithLifecycle()
    val durationMs by viewModel.recordingDurationMs.collectAsStateWithLifecycle()
    
    val vocalPitch by viewModel.vocalPitch.collectAsStateWithLifecycle()
    val vocalSpeed by viewModel.vocalSpeed.collectAsStateWithLifecycle()
    val noiseCancel by viewModel.noiseCancellationActive.collectAsStateWithLifecycle()
    val selectedRecord by viewModel.selectedRecording.collectAsStateWithLifecycle()
    val activePlayingId by viewModel.activePlayingId.collectAsStateWithLifecycle()

    var currentTimeString by remember { mutableStateOf("") }

    // Live Clock ticking effect
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss.S", Locale.getDefault())
            currentTimeString = "UTC_FEED: " + sdf.format(Date())
            kotlinx.coroutines.delay(100)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Futuristic HUD Header Layout
        HeaderHudBlock(currentTimeString = currentTimeString, isRecording = isRecording, noiseCancel = noiseCancel)

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Animated Rotating Arc Reactor / wave visualizer
        InteractiveArcReactor(
            isRecording = isRecording,
            isPlaying = isPlaying,
            amplitude = liveAmplitude,
            durationMs = durationMs
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Real-time Transcription Screen
        AnimatedVisibility(
            visible = isRecording || currentTranscription.isNotEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            LiveTranscriptionTerminal(transcriptText = currentTranscription, isRecording = isRecording)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Central Recording Button Panel
        RecordingTriggerPanel(
            isRecording = isRecording,
            noiseCancel = noiseCancel,
            onToggleNoiseCancel = { viewModel.setNoiseCancellation(it) },
            onStartRecording = { viewModel.startRecording() },
            onStopRecording = { viewModel.stopRecording() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Voice Processing Sliders Deck (Pitch & Speed)
        VoiceEffectsProcessorDeck(
            pitchValue = vocalPitch,
            speedValue = vocalSpeed,
            onPitchChange = { viewModel.setPitch(it) },
            onSpeedChange = { viewModel.setSpeed(it) },
            onSaveProfile = {
                selectedRecord?.let { viewModel.updateVocalEffectsForSelected(it) }
            },
            isProfileSavedEnabled = selectedRecord != null
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Selected Recording & Jarvis AI Diagnostics Pane
        selectedRecord?.let { record ->
            SelectedRecordDetailsPane(
                recording = record,
                isPlaying = isPlaying && activePlayingId == record.id,
                vocalPitch = vocalPitch,
                vocalSpeed = vocalSpeed,
                aiLoading = viewModel.isAILoading.collectAsStateWithLifecycle().value,
                aiResultText = viewModel.aiResultText.collectAsStateWithLifecycle().value,
                onPlay = { viewModel.playRecording(record) },
                onStop = { viewModel.stopPlaying() },
                onDelete = { viewModel.deleteRecording(record) },
                onTriggerAI = { type -> viewModel.analyzeVocalReference(record, type) }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 6. Archived Sound Telemetry Log
        ArchivedLogsPanel(
            recordings = recordings,
            activePlayingId = activePlayingId,
            selectedId = selectedRecord?.id,
            onSelect = { viewModel.selectRecording(it) },
            onPlayToggle = { viewModel.playRecording(it) },
            onDelete = { viewModel.deleteRecording(it) }
        )
    }
}

@Composable
fun HeaderHudBlock(currentTimeString: String, isRecording: Boolean, noiseCancel: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Drawing elegant bottom border lines instead of heavy grids
                drawLine(
                    color = JarvisPrimaryNeon.copy(alpha = 0.2f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // w-8 h-8 rounded-lg bg-[#00F0FF]/10 flex items-center justify-center border border-[#00F0FF]/30
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisPrimaryNeon.copy(alpha = 0.1f))
                        .border(1.dp, JarvisPrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AI",
                        color = JarvisPrimaryNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Column {
                    Text(
                        text = "AURA SYSTEMS",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "QUANTUM RECORDING V2.4",
                        color = JarvisPrimaryNeon.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right Group: NC status and Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ANC Badge: bg-[#00F0FF]/5 px-2.5 py-1 rounded-full border border-[#00F0FF]/20
                val infiniteTransition = rememberInfiniteTransition(label = "AncPulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseAlpha"
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            if (noiseCancel) JarvisPrimaryNeon.copy(alpha = 0.05f) 
                            else JarvisOnSurface.copy(alpha = 0.05f)
                        )
                        .border(
                            0.8.dp,
                            if (noiseCancel) JarvisPrimaryNeon.copy(alpha = 0.2f) 
                            else JarvisOnSurface.copy(alpha = 0.15f),
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (noiseCancel) JarvisPrimaryNeon.copy(alpha = pulseAlpha)
                                else JarvisOnSurface.copy(alpha = 0.5f)
                            )
                    )
                    Text(
                        text = if (noiseCancel) "NC-ON" else "NC-OFF",
                        color = if (noiseCancel) JarvisPrimaryNeon else JarvisOnSurface,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Clean clock indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currentTimeString.replace("UTC_FEED: ", ""),
                        color = JarvisOnSurface,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveArcReactor(
    isRecording: Boolean,
    isPlaying: Boolean,
    amplitude: Float,
    durationMs: Long
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Arc Reactor")
    
    // Smooth angle rotation
    val angleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRecording) 1500 else 6000, easing = LinearEasing)
        ),
        label = "Angle Rotation"
    )

    // Pulse factor for bars
    val dynamicAmpMultiplier = if (isRecording || isPlaying) amplitude else 0.05f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circle 1 (Outer Circle): bg-transparent, border_cyan/5
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .border(1.dp, JarvisPrimaryNeon.copy(alpha = 0.05f), CircleShape)
            )

            // Circle 2 (Middle Circle): border_cyan/10
            Box(
                modifier = Modifier
                    .size(165.dp)
                    .clip(CircleShape)
                    .border(1.dp, JarvisPrimaryNeon.copy(alpha = 0.12f), CircleShape)
            )

            // Canvas for rotating outer notches positioned perfectly
            Canvas(modifier = Modifier.size(165.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.minDimension * 0.48f

                rotate(angleRotation) {
                    // Outer ticked neon ring Segment
                    drawArc(
                        color = JarvisPrimaryNeon.copy(alpha = 0.4f),
                        startAngle = 10f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        size = size.copy(width = outerRadius * 2, height = outerRadius * 2),
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
                    )
                    
                    drawArc(
                        color = JarvisPrimaryNeon.copy(alpha = 0.2f),
                        startAngle = 130f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        size = size.copy(width = outerRadius * 2, height = outerRadius * 2),
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
                    )

                    drawArc(
                        color = JarvisPrimaryNeon.copy(alpha = 0.4f),
                        startAngle = 250f,
                        sweepAngle = 80f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        size = size.copy(width = outerRadius * 2, height = outerRadius * 2),
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
                    )
                }
            }

            // Circle 3 (Central Neon Core Circle): bg-gradient, border_cyan/40, shadow
            Box(
                modifier = Modifier
                    .size(115.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                JarvisPrimaryNeon.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.2.dp, JarvisPrimaryNeon.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Waveform vertical bars layout
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(48.dp)
                ) {
                    // Bar 1
                    Spacer(
                        modifier = Modifier
                            .width(4.dp)
                            .height(8.dp + (24 * dynamicAmpMultiplier).dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(JarvisPrimaryNeon.copy(alpha = 0.4f))
                    )
                    // Bar 2
                    Spacer(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp + (30 * dynamicAmpMultiplier).dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(JarvisPrimaryNeon.copy(alpha = 0.6f))
                    )
                    // Bar 3 (Core)
                    Spacer(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp + (42 * dynamicAmpMultiplier).dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(JarvisPrimaryNeon)
                    )
                    // Bar 4
                    Spacer(
                        modifier = Modifier
                            .width(4.dp)
                            .height(14.dp + (26 * dynamicAmpMultiplier).dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(JarvisPrimaryNeon.copy(alpha = 0.9f))
                    )
                    // Bar 5
                    Spacer(
                        modifier = Modifier
                            .width(4.dp)
                            .height(22.dp + (34 * dynamicAmpMultiplier).dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(JarvisPrimaryNeon.copy(alpha = 0.8f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large high-contrast state time counters
        Text(
            text = formatDuration(durationMs),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun LiveTranscriptionTerminal(transcriptText: String, isRecording: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 160.dp)
            .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME TRANSCRIPTION",
                    color = JarvisOnSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                
                Text(
                    text = if (isRecording) "LIVE STREAMING" else "CONFIDENCE: 98.4%",
                    color = JarvisPrimaryNeon,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row {
                    Text(
                        text = buildString {
                            append("...environment status: calibrated. ")
                            if (transcriptText.isEmpty() && isRecording) {
                                append("\"Awaiting signal stream... Speak clearly into microphone node.\"")
                            } else if (transcriptText.isEmpty()) {
                                append("\"Vocal audio logs empty.\"")
                            } else {
                                append("\"")
                                append(transcriptText)
                                append("\"")
                            }
                        },
                        color = Color(0xFFCBD5E1),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.testTag("transcription_text")
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingTriggerPanel(
    isRecording: Boolean,
    noiseCancel: Boolean,
    onToggleNoiseCancel: (Boolean) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Elegant Dark circular White CTA with pulsing background glow
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing background glow: inset blur simulation
            val infiniteTransition = rememberInfiniteTransition(label = "CtaPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseScale"
            )
            val glowColor = if (isRecording) JarvisTertiaryOrange else JarvisPrimaryNeon

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .drawBehind {
                        drawCircle(
                            color = glowColor.copy(alpha = 0.15f * (2f - pulseScale)),
                            radius = (size.minDimension / 2) * pulseScale
                        )
                    }
            )

            // High contrast white circular button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        if (isRecording) onStopRecording() else onStartRecording()
                    }
                    .testTag("record_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    // Small stop black square in the center: w-6 h-6 rounded bg-[#05070A]
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(JarvisBackground)
                    )
                } else {
                    // Modern mic icon styled in JarvisBackground black/dark color
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Request Vocal Scan",
                        tint = JarvisBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Noise suppression toggle switch: rounded-3xl (24.dp) with white/5 border
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (noiseCancel) Icons.Default.Hearing else Icons.Default.VolumeUp,
                        contentDescription = "Noise Isolation Status",
                        tint = if (noiseCancel) JarvisPrimaryNeon else JarvisOnSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "NOISE ISOLATION (ANC)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (noiseCancel) "Dampening ambient acoustic vibration" else "Recording raw microphone feed",
                            color = JarvisOnSurface,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Switch(
                    checked = noiseCancel,
                    onCheckedChange = onToggleNoiseCancel,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisBackground,
                        checkedTrackColor = JarvisPrimaryNeon,
                        uncheckedThumbColor = JarvisOnSurface,
                        uncheckedTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.testTag("noise_cancellation_switch")
                )
            }
        }
    }
}

@Composable
fun VoiceEffectsProcessorDeck(
    pitchValue: Float,
    speedValue: Float,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSaveProfile: () -> Unit,
    isProfileSavedEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "VOCAL RESYNTHESIS DECK (PITCH & TONE)",
                color = JarvisOnSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Pitch synthesis Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VOCAL PITCH STABILIZER",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = String.format("%.2f ST", pitchValue - 1.0f),
                    color = JarvisPrimaryNeon,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Slider(
                value = pitchValue,
                onValueChange = onPitchChange,
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisPrimaryNeon,
                    activeTrackColor = JarvisPrimaryNeon,
                    inactiveTrackColor = Color(0x12FFFFFF),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.testTag("pitch_slider")
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sub-Oscillator (Robotic)", color = JarvisOnSurface, fontSize = 9.sp)
                Text("Hyper-Resonant (Vocal Chipmunk)", color = JarvisOnSurface, fontSize = 9.sp)
            }

            // 2. Speed synthesis Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TEMPORAL TIMBRE FREQUENCY",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = String.format("%.2fx Speed", speedValue),
                    color = JarvisPrimaryNeon,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Slider(
                value = speedValue,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisPrimaryNeon,
                    activeTrackColor = JarvisPrimaryNeon,
                    inactiveTrackColor = Color(0x12FFFFFF),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.testTag("speed_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Chronology Dilated", color = JarvisOnSurface, fontSize = 9.sp)
                Text("Frequency Accelerated", color = JarvisOnSurface, fontSize = 9.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save modified parameters - high-contrast pill style CTA
            Button(
                onClick = onSaveProfile,
                enabled = isProfileSavedEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisPrimaryNeon,
                    contentColor = JarvisBackground,
                    disabledContainerColor = Color(0x06FFFFFF),
                    disabledContentColor = JarvisOnSurface.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("save_effects_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save vocal profiles",
                    tint = if (isProfileSavedEnabled) JarvisBackground else JarvisOnSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SAVE SOUND PROFILE TO FILE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SelectedRecordDetailsPane(
    recording: RecordingEntity,
    isPlaying: Boolean,
    vocalPitch: Float,
    vocalSpeed: Float,
    aiLoading: Boolean,
    aiResultText: String?,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onTriggerAI: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisPrimaryNeon.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE TELEMETRY LOADED",
                        color = JarvisPrimaryNeon,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = recording.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(38.dp)
                            .background(JarvisPrimaryNeon.copy(alpha = 0.12f), CircleShape)
                            .testTag("selected_play_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = JarvisPrimaryNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(38.dp)
                            .background(JarvisTertiaryOrange.copy(alpha = 0.12f), CircleShape)
                            .testTag("selected_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Expunge reference archive",
                            tint = JarvisTertiaryOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0x12FFFFFF), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // File statistics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("SAMPLE FORMAT", color = JarvisOnSurface, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("MPEG_4 (AAC)", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("ISOLATION STATE", color = JarvisOnSurface, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(if (recording.isNoiseCancelled) "ANC ACTIVE" else "RAW FEED", color = if (recording.isNoiseCancelled) JarvisPrimaryNeon else JarvisTertiaryOrange, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("TIMESTAMP INDEX", color = JarvisOnSurface, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    val dateStr = remember(recording.timestamp) {
                        val sdf = SimpleDateFormat("HH:mm:ss dd-MMM", Locale.getDefault())
                        sdf.format(Date(recording.timestamp))
                    }
                    Text(dateStr, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Real AI diagnostic tools trigger buttons
            Text(
                text = "J.A.R.V.I.S. INTELLIGENCE DIAGNOSTIC UNIT",
                color = JarvisOnSurface,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onTriggerAI("summary") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisPrimaryNeon.copy(alpha = 0.05f),
                        contentColor = JarvisPrimaryNeon
                    ),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(0.8.dp, JarvisPrimaryNeon.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("ai_summary_button")
                ) {
                    Text("GENERATE BRIEF", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onTriggerAI("sci_fi") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisPrimaryNeon.copy(alpha = 0.05f),
                        contentColor = JarvisPrimaryNeon
                    ),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(0.8.dp, JarvisPrimaryNeon.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("ai_sci_fi_button")
                ) {
                    Text("STARK REWRITE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onTriggerAI("tone") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisPrimaryNeon.copy(alpha = 0.05f),
                        contentColor = JarvisPrimaryNeon
                    ),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(0.8.dp, JarvisPrimaryNeon.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("ai_tone_button")
                ) {
                    Text("TONE PROFILE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            // AI Response glowing terminal monitor
            aiResultText?.let { aiText ->
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF00C853).copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0x0600C853)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HOLOGRAPHIC DIAGNOSTIC FEED",
                                color = Color(0xFF00C853),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (aiLoading) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00C853),
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Text(
                                    text = "SECURED",
                                    color = Color(0xFF00C853).copy(alpha = 0.5f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color(0x1F00C853), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = aiText,
                            color = Color(0xFF69F0AE),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                                .testTag("ai_terminal_result")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivedLogsPanel(
    recordings: List<RecordingEntity>,
    activePlayingId: Int?,
    selectedId: Int?,
    onSelect: (RecordingEntity) -> Unit,
    onPlayToggle: (RecordingEntity) -> Unit,
    onDelete: (RecordingEntity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SECURED SOUND TELEMETRY ARCHIVE",
                color = JarvisOnSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "${recordings.size} ARCHIVES",
                color = JarvisPrimaryNeon,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(24.dp))
                    .background(JarvisSurface)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Archive completely vacant",
                        tint = JarvisOnSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Vocal archives vacant. Please record core speech.",
                        color = JarvisOnSurface,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recordings.forEach { record ->
                    ArchiveRecordCard(
                        recording = record,
                        isPlaying = activePlayingId == record.id,
                        isSelected = selectedId == record.id,
                        onSelect = { onSelect(record) },
                        onPlayToggle = { onPlayToggle(record) },
                        onDelete = { onDelete(record) }
                    )
                }
            }
        }
    }
}

@Composable
fun ArchiveRecordCard(
    recording: RecordingEntity,
    isPlaying: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPlayToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = 1.dp,
                color = if (isSelected) JarvisPrimaryNeon.copy(alpha = 0.4f) else JarvisSurfaceBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("archive_card_${recording.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) JarvisSurface.copy(alpha = 0.85f) else JarvisSurface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle button matching the design aesthetic
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) JarvisPrimaryNeon.copy(alpha = 0.15f) else Color(0x12FFFFFF)
                    )
                    .clickable { onPlayToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Start",
                    tint = if (isPlaying) JarvisPrimaryNeon else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1.0f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recording.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (recording.isNoiseCancelled) {
                        Text(
                            text = "ANC",
                            color = JarvisPrimaryNeon,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .background(JarvisPrimaryNeon.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DURATION: ${formatDuration(recording.durationMs)}",
                        color = JarvisOnSurface,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "• P:${recording.pitch}x S:${recording.speed}x",
                        color = JarvisOnSurface,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Secondary quick delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("quick_delete_${recording.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Purge reference record",
                    tint = JarvisOnSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 60000) % 60
    return String.format("%02d:%02d", min, sec)
}
