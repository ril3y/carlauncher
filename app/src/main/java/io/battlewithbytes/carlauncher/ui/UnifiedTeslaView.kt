package io.battlewithbytes.carlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.battlewithbytes.carlauncher.R
import io.battlewithbytes.carlauncher.viewmodel.VehicleDataViewModel
import kotlin.math.roundToInt

/**
 * Unified Tesla-style interface - single canvas with all info
 */
@Composable
fun UnifiedTeslaView(
    vehicleDataViewModel: VehicleDataViewModel = viewModel()
) {
    val batteryData by vehicleDataViewModel.batteryData.collectAsState()
    val motorData by vehicleDataViewModel.motorData.collectAsState()
    val vehicleStatus by vehicleDataViewModel.vehicleStatus.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showMedia by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main content area - Tesla Model 3/Y style
            // Left sidebar extends to top, status bar only on right side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Left: Compact info sidebar (~30% - 15% wider) - extends to top
                LeftSidebarFullHeight(
                    speed = vehicleStatus?.speedKmh ?: 0f,
                    batteryPercent = batteryData?.stateOfChargePercent ?: 0f,
                    range = batteryData?.let { (it.stateOfChargePercent / 100f) * 80f } ?: 0f,
                    powerKw = motorData?.powerKw ?: 0f,
                    voltage = batteryData?.voltageV ?: 0f,
                    current = batteryData?.currentA ?: 0f,
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                )

                // Right: Status bar + Map area (~70%)
                Column(
                    modifier = Modifier
                        .weight(2.1f)
                        .fillMaxHeight()
                ) {
                    // Top status bar (only on right side now)
                    TeslaStatusBarUnified(
                        batteryPercent = batteryData?.stateOfChargePercent ?: 0f,
                        temperature = batteryData?.temperatureC ?: 0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Map area with media and settings overlays
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Map
                        MapWithCarOverlay(
                            speed = vehicleStatus?.speedKmh ?: 0f,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Media overlay (lower z-index)
                        if (showMedia) {
                            MediaPlayerWidget(
                                onDismiss = { showMedia = false }
                            )
                        }

                        // Settings overlay (higher z-index - appears on top)
                        if (showSettings) {
                            SettingsScreen(
                                onDismiss = { showSettings = false }
                            )
                        }
                    }
                }
            }

            // Bottom status bar (always visible)
            BottomStatusBar(
                range = batteryData?.let { (it.stateOfChargePercent / 100f) * 80f } ?: 0f,
                power = motorData?.powerKw ?: 0f,
                gear = "D",
                onControlsClick = { showSettings = true },
                onCarIconClick = { showSettings = !showSettings },
                onMediaClick = { showMedia = !showMedia },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Media bar overlay - positioned on right side above bottom bar with gap
        // Hide when media player overlay is open
        if (!showMedia) {
            MediaBar(
                onClick = { showMedia = !showMedia },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp) // Raised higher to show map gap
            )
        }
    }
}

@Composable
fun TeslaStatusBarUnified(
    batteryPercent: Float,
    temperature: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right: Time & Temperature only (battery is in sidebar)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getCurrentTime(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "${temperature.toInt()}°C",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF808080)
                )
            }
        }
    }
}

@Composable
fun LeftSidebarFullHeight(
    speed: Float,
    batteryPercent: Float,
    range: Float,
    powerKw: Float,
    voltage: Float,
    current: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0A0A0A))
            .padding(vertical = 16.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top: Battery indicator (replaces status bar on this side)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = when {
                    batteryPercent > 60f -> Color(0xFF4CAF50)
                    batteryPercent > 20f -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                },
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "${batteryPercent.toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large speed display - most prominent
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = speed.toInt().toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "km/h",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF808080)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Gear selector - P R N D
        Text(
            text = "P R N D",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // Battery & Range info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Range
            Text(
                text = "${range.toInt()} km",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF808080)
            )

            // Golf cart image
            Image(
                painter = painterResource(id = R.drawable.gcart),
                contentDescription = "Golf Cart",
                modifier = Modifier
                    .size(150.dp)
                    .padding(vertical = 8.dp)
            )

            // 3 placeholder buttons (to be connected later)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Button 1 - Lightbulb
                Button(
                    onClick = { /* TODO: Connect later */ },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A2A),
                        contentColor = Color(0xFFFFFFFF)
                    ),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Button 1",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Button 2 - Navigation
                Button(
                    onClick = { /* TODO: Connect later */ },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A2A),
                        contentColor = Color(0xFFFFFFFF)
                    ),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Button 2",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Button 3 - Settings
                Button(
                    onClick = { /* TODO: Connect later */ },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A2A),
                        contentColor = Color(0xFFFFFFFF)
                    ),
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Button 3",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MapWithCarOverlay(
    speed: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF0A0A0A))
            .fillMaxSize()
    ) {
        // Real OSMDroid map with GPS tracking - dark mode enabled
        MapView(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(), // Ensure map stays within bounds
            isDarkMode = true // Tesla dark theme
        )
    }
}

@Composable
fun CarVisualizationPerspective(modifier: Modifier = Modifier) {
    // Real golf cart image with solar panels
    Image(
        painter = painterResource(id = R.drawable.gcart),
        contentDescription = "Golf Cart",
        modifier = modifier,
        colorFilter = ColorFilter.tint(Color(0xFF3E6AE1).copy(alpha = 0.9f))
    )
}

@Composable
fun MetricRowCompact(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF606060)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB0B0B0),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PowerGraph(powerKw: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Background grid
        for (i in 0..4) {
            val y = height * i / 4f
            drawLine(
                color = Color(0xFF1A1A1A),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Simulated power curve (will be replaced with real-time data)
        val points = listOf(
            Offset(0f, height * 0.7f),
            Offset(width * 0.2f, height * 0.5f),
            Offset(width * 0.4f, height * 0.6f),
            Offset(width * 0.6f, height * 0.3f),
            Offset(width * 0.8f, height * 0.4f),
            Offset(width, height * (1f - powerKw / 5f))
        )

        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = path,
            color = Color(0xFFFF9800),
            style = Stroke(width = 3f)
        )
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF808080)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MediaBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Swipeable media bar overlay - positioned on right side
    var isExpanded by remember { mutableStateOf(true) }
    var offsetY by remember { mutableStateOf(0f) }

    if (isExpanded) {
        Surface(
            modifier = modifier
                .widthIn(min = 400.dp, max = 600.dp)
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .clickable { onClick() } // Click to open media overlay
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetY > 100f) {
                                isExpanded = false
                            } else {
                                offsetY = 0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                        }
                    )
                },
            color = Color(0xFF1A1A1A),
            tonalElevation = 8.dp,
            shape = RectangleShape // No rounded edges
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art + song info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Album art placeholder
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF2A2A2A), MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF606060),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Song title & artist
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "No media playing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Connect Bluetooth or USB",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF808080)
                        )
                    }
                }

                // Playback controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Previous */ }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color(0xFF808080),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = { /* Play/Pause */ }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { /* Next */ }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color(0xFF808080),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Close button
                IconButton(onClick = { isExpanded = false }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Hide media bar",
                        tint = Color(0xFF808080)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomStatusBar(
    range: Float,
    power: Float,
    gear: String,
    onControlsClick: () -> Unit,
    onCarIconClick: () -> Unit,
    onMediaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0A0A0A),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Car icon (Tesla style)
            BottomIconButton(Icons.Default.DirectionsCar, "Cart", onClick = onCarIconClick)

            // Center: Icon buttons menu (Tesla style)
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomIconButton(Icons.Default.Lock, "Lock", onClick = {})
                BottomIconButton(Icons.Default.AcUnit, "Climate", onClick = {})
                BottomIconButton(Icons.Default.BatteryChargingFull, "Charging", onClick = {})
                BottomIconButton(Icons.Default.Settings, "Controls", onClick = onControlsClick)
                BottomIconButton(Icons.Default.Headphones, "Media", onClick = onMediaClick)
            }

            // Right: Spacer for balance
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
fun BottomIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF808080),
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF606060),
            fontSize = 10.sp
        )
    }
}

private fun getCurrentTime(): String {
    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}
