package io.battlewithbytes.carlauncher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Tesla-style three-column configuration screen
 * Overlays the map area (70% right side) while keeping sidebar and bottom bar visible
 */
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    telemetryViewModel: io.battlewithbytes.carlauncher.viewmodel.TelemetryViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedCategory by remember { mutableStateOf<ConfigCategory?>(configCategories.firstOrNull()) }
    var selectedOption by remember { mutableStateOf<ConfigOption?>(null) }

    // Observe telemetry data
    val telemetryData by telemetryViewModel.telemetryData.collectAsState()

    // Get screen height for offset calculations
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Track drag offset - initialized based on visible state
    var offsetY by remember(visible) {
        mutableStateOf(if (visible) 0f else screenHeight)
    }

    // Animate offset with spring for natural feel
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SettingsOffset"
    )

    // Update offset when visibility changes
    LaunchedEffect(visible) {
        offsetY = if (visible) 0f else screenHeight
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .background(Color.Black.copy(alpha = 0.97f))
            .pointerInput(visible) {
                if (visible) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // Allow dragging down, and up only when partially dragged
                            offsetY = (offsetY + dragAmount.toFloat()).coerceIn(0f, screenHeight)
                        },
                        onDragEnd = {
                            // Snap logic: if dragged more than 50% down, dismiss
                            if (offsetY > screenHeight * 0.5f) {
                                offsetY = screenHeight
                                onDismiss()
                            } else {
                                // Snap back to open
                                offsetY = 0f
                            }
                        }
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with swipe indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Swipe indicator bar
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(Color(0xFF404040), RoundedCornerShape(2.dp))
                )
            }

            // Three-column layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Left column: Categories
                CategoryColumn(
                    categories = configCategories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        selectedCategory = category
                        selectedOption = null // Reset option when category changes
                    },
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight()
                )

                // Middle column: Options for selected category
                OptionColumn(
                    category = selectedCategory,
                    selectedOption = selectedOption,
                    onOptionSelected = { option ->
                        selectedOption = option
                    },
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                )

                // Right column: Detail panel for selected option
                DetailPanel(
                    option = selectedOption,
                    telemetryData = telemetryData,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun CategoryColumn(
    categories: List<ConfigCategory>,
    selectedCategory: ConfigCategory?,
    onCategorySelected: (ConfigCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0A0A0A))
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Controls",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(categories) { category ->
                CategoryItem(
                    category = category,
                    isSelected = category == selectedCategory,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: ConfigCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF1A1A1A) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color(0xFF808080)
    val iconColor = if (isSelected) Color(0xFF3E6AE1) else Color(0xFF606060)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun OptionColumn(
    category: ConfigCategory?,
    selectedOption: ConfigOption?,
    onOptionSelected: (ConfigOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF050505))
            .padding(vertical = 8.dp)
    ) {
        if (category != null) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(category.options) { option ->
                    OptionItem(
                        option = option,
                        isSelected = option == selectedOption,
                        onClick = { onOptionSelected(option) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select a category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF606060)
                )
            }
        }
    }
}

@Composable
fun OptionItem(
    option: ConfigOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFF1A1A1A) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color(0xFF808080)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (option.currentValue != null) {
                    Text(
                        text = option.currentValue,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF606060),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (option.hasDetail) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF606060),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DetailPanel(
    option: ConfigOption?,
    telemetryData: io.battlewithbytes.carlauncher.service.TelemetryData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .padding(16.dp)
    ) {
        if (option != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = option.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF808080),
                        lineHeight = 20.sp
                    )
                }

                // Render different control types based on option
                when (option.controlType) {
                    ControlType.GPS_DETAIL -> {
                        item {
                            GpsDetailControl(telemetryData = telemetryData)
                        }
                    }
                    ControlType.TOGGLE -> {
                        item {
                            DetailToggleControl(
                                label = option.name,
                                value = option.currentValue == "On",
                                onValueChange = { /* TODO: Handle toggle */ }
                            )
                        }
                    }
                    ControlType.SLIDER -> {
                        item {
                            DetailSliderControl(
                                label = option.name,
                                value = option.currentValue?.toFloatOrNull() ?: 50f,
                                valueRange = 0f..100f,
                                onValueChange = { /* TODO: Handle slider */ }
                            )
                        }
                    }
                    ControlType.SELECTOR -> {
                        item {
                            DetailSelectorControl(
                                label = option.name,
                                options = option.selectorOptions ?: emptyList(),
                                selectedOption = option.currentValue ?: "",
                                onOptionSelected = { /* TODO: Handle selection */ }
                            )
                        }
                    }
                    ControlType.INFO -> {
                        item {
                            DetailInfoControl(
                                items = option.infoItems ?: emptyList()
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF303030),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Select an option to configure",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF606060)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailToggleControl(
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Switch(
                checked = value,
                onCheckedChange = onValueChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3E6AE1),
                    uncheckedThumbColor = Color(0xFF606060),
                    uncheckedTrackColor = Color(0xFF2A2A2A)
                )
            )
        }
    }
}

@Composable
fun DetailSliderControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "${value.toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF3E6AE1),
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF3E6AE1),
                    inactiveTrackColor = Color(0xFF2A2A2A)
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun DetailSelectorControl(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option) },
                color = if (option == selectedOption) Color(0xFF3E6AE1).copy(alpha = 0.2f) else Color(0xFF1A1A1A),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (option == selectedOption) Color.White else Color(0xFF808080)
                    )
                    if (option == selectedOption) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF3E6AE1),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailInfoControl(
    items: List<InfoItem>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF808080)
                    )
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun GpsDetailControl(
    telemetryData: io.battlewithbytes.carlauncher.service.TelemetryData
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection Status Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Status indicator dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = Color(telemetryData.getStatusColor()),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                        Text(
                            text = telemetryData.getSignalQuality(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(telemetryData.getStatusColor()),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Fix Type and Satellite Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Fix Type",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF808080)
                        )
                        Text(
                            text = when (telemetryData.fixType) {
                                io.battlewithbytes.carlauncher.service.GpsFixType.NO_FIX -> "No Fix"
                                io.battlewithbytes.carlauncher.service.GpsFixType.FIX_2D -> "2D Fix"
                                io.battlewithbytes.carlauncher.service.GpsFixType.FIX_3D -> "3D Fix"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Satellites",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF808080)
                        )
                        Text(
                            text = if (telemetryData.satellitesUsed > 0) {
                                "${telemetryData.satellitesUsed} / ${telemetryData.satellitesInView}"
                            } else "Searching...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Data staleness warning
                if (telemetryData.isStale()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF39C12).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF39C12),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "GPS data is stale (>5s old)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF39C12)
                        )
                    }
                }
            }
        }

        // Accuracy Metrics Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Accuracy Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                // Horizontal Accuracy
                GpsMetricRow(
                    label = "Horizontal Accuracy",
                    value = if (telemetryData.accuracy > 0) {
                        String.format("%.1f m", telemetryData.accuracy)
                    } else "N/A",
                    quality = when {
                        telemetryData.accuracy == 0f -> "N/A"
                        telemetryData.accuracy < 5f -> "Excellent"
                        telemetryData.accuracy < 10f -> "Good"
                        telemetryData.accuracy < 20f -> "Fair"
                        else -> "Poor"
                    }
                )

                // Vertical Accuracy
                GpsMetricRow(
                    label = "Vertical Accuracy",
                    value = if (telemetryData.verticalAccuracy > 0) {
                        String.format("%.1f m", telemetryData.verticalAccuracy)
                    } else "N/A",
                    quality = when {
                        telemetryData.verticalAccuracy == 0f -> "N/A"
                        telemetryData.verticalAccuracy < 10f -> "Excellent"
                        telemetryData.verticalAccuracy < 20f -> "Good"
                        telemetryData.verticalAccuracy < 50f -> "Fair"
                        else -> "Poor"
                    }
                )

                // HDOP
                GpsMetricRow(
                    label = "HDOP",
                    value = if (telemetryData.hdop < 99f) {
                        String.format("%.1f", telemetryData.hdop)
                    } else "N/A",
                    quality = when {
                        telemetryData.hdop >= 99f -> "N/A"
                        telemetryData.hdop < 2f -> "Excellent"
                        telemetryData.hdop < 5f -> "Good"
                        telemetryData.hdop < 10f -> "Fair"
                        else -> "Poor"
                    }
                )

                // VDOP
                GpsMetricRow(
                    label = "VDOP",
                    value = if (telemetryData.vdop < 99f) {
                        String.format("%.1f", telemetryData.vdop)
                    } else "N/A",
                    quality = when {
                        telemetryData.vdop >= 99f -> "N/A"
                        telemetryData.vdop < 2f -> "Excellent"
                        telemetryData.vdop < 5f -> "Good"
                        telemetryData.vdop < 10f -> "Fair"
                        else -> "Poor"
                    }
                )
            }
        }

        // Location Data Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Location Data",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                // Latitude
                GpsDataRow(
                    label = "Latitude",
                    value = if (telemetryData.latitude != 0.0) {
                        String.format("%.7f°", telemetryData.latitude)
                    } else "0.0000000°"
                )

                // Longitude
                GpsDataRow(
                    label = "Longitude",
                    value = if (telemetryData.longitude != 0.0) {
                        String.format("%.7f°", telemetryData.longitude)
                    } else "0.0000000°"
                )

                // Altitude
                GpsDataRow(
                    label = "Altitude",
                    value = if (telemetryData.altitude != 0.0) {
                        String.format("%.1f m", telemetryData.altitude)
                    } else "N/A"
                )

                // Speed
                GpsDataRow(
                    label = "Ground Speed",
                    value = String.format("%.1f km/h", telemetryData.groundSpeed)
                )

                // Bearing
                GpsDataRow(
                    label = "Bearing",
                    value = if (telemetryData.bearing > 0) {
                        String.format("%.1f° (%s)", telemetryData.bearing, getBearingDirection(telemetryData.bearing))
                    } else "N/A"
                )
            }
        }

        // Timestamp Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Time Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                GpsDataRow(
                    label = "GPS Time",
                    value = if (telemetryData.timestamp > 0) {
                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(telemetryData.timestamp))
                    } else "N/A"
                )

                GpsDataRow(
                    label = "Last Update",
                    value = if (telemetryData.timestamp > 0) {
                        val ageSeconds = (System.currentTimeMillis() - telemetryData.timestamp) / 1000
                        "$ageSeconds seconds ago"
                    } else "Never"
                )
            }
        }
    }
}

@Composable
private fun GpsMetricRow(
    label: String,
    value: String,
    quality: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
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
                text = quality,
                style = MaterialTheme.typography.bodySmall,
                color = when (quality) {
                    "Excellent" -> Color(0xFF2ECC71)
                    "Good" -> Color(0xFF3E6AE1)
                    "Fair" -> Color(0xFFF39C12)
                    "Poor" -> Color(0xFFE74C3C)
                    else -> Color(0xFF606060)
                },
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GpsDataRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF808080)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun getBearingDirection(bearing: Float): String {
    return when {
        bearing < 22.5 || bearing >= 337.5 -> "N"
        bearing < 67.5 -> "NE"
        bearing < 112.5 -> "E"
        bearing < 157.5 -> "SE"
        bearing < 202.5 -> "S"
        bearing < 247.5 -> "SW"
        bearing < 292.5 -> "W"
        bearing < 337.5 -> "NW"
        else -> "N"
    }
}

// Data models
data class ConfigCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val options: List<ConfigOption>
)

data class ConfigOption(
    val id: String,
    val name: String,
    val description: String,
    val currentValue: String? = null,
    val hasDetail: Boolean = true,
    val controlType: ControlType = ControlType.INFO,
    val selectorOptions: List<String>? = null,
    val infoItems: List<InfoItem>? = null
)

data class InfoItem(
    val label: String,
    val value: String
)

enum class ControlType {
    TOGGLE,
    SLIDER,
    SELECTOR,
    INFO,
    GPS_DETAIL
}

// Configuration categories and options for golf cart
val configCategories = listOf(
    ConfigCategory(
        id = "pedals_steering",
        name = "Pedals & Steering",
        icon = Icons.Default.DirectionsCar,
        options = listOf(
            ConfigOption(
                id = "steering_mode",
                name = "Steering Mode",
                description = "Adjust steering feel and response for different driving conditions",
                currentValue = "Comfort",
                controlType = ControlType.SELECTOR,
                selectorOptions = listOf("Comfort", "Standard", "Sport")
            ),
            ConfigOption(
                id = "pedal_response",
                name = "Accelerator Pedal",
                description = "Control how quickly the cart responds to accelerator input",
                currentValue = "Standard",
                controlType = ControlType.SELECTOR,
                selectorOptions = listOf("Chill", "Standard", "Sport")
            ),
            ConfigOption(
                id = "regen_braking",
                name = "Regenerative Braking",
                description = "Adjust how much the cart slows down when you lift off the pedal",
                currentValue = "Standard",
                controlType = ControlType.SELECTOR,
                selectorOptions = listOf("Low", "Standard", "High")
            ),
            ConfigOption(
                id = "steering_calibration",
                name = "Steering Calibration",
                description = "Calibrate steering wheel center position and range",
                currentValue = "Run calibration",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Last calibrated", "Never"),
                    InfoItem("Status", "Ready")
                )
            )
        )
    ),
    ConfigCategory(
        id = "charging",
        name = "Charging",
        icon = Icons.Default.BatteryChargingFull,
        options = listOf(
            ConfigOption(
                id = "charging_current",
                name = "Charge Current Limit",
                description = "Set maximum charging current to protect battery longevity",
                currentValue = "80",
                controlType = ControlType.SLIDER
            ),
            ConfigOption(
                id = "solar_panel",
                name = "Solar Panel",
                description = "Monitor and configure roof-mounted solar charging system",
                currentValue = "Active - 45W",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Current Output", "45W"),
                    InfoItem("Today's Energy", "0.8 kWh"),
                    InfoItem("Panel Temp", "32°C"),
                    InfoItem("Efficiency", "18%")
                )
            ),
            ConfigOption(
                id = "charge_scheduling",
                name = "Scheduled Charging",
                description = "Set times when the cart should charge from external sources",
                currentValue = "Off",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "charge_port",
                name = "Charge Port Door",
                description = "Open or close the charge port door",
                currentValue = "Closed",
                controlType = ControlType.TOGGLE
            )
        )
    ),
    ConfigCategory(
        id = "autopilot",
        name = "Autopilot",
        icon = Icons.Default.Psychology,
        options = listOf(
            ConfigOption(
                id = "autopilot_status",
                name = "Autopilot",
                description = "Autonomous navigation features - Coming Soon",
                currentValue = "Not Available",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Status", "Development"),
                    InfoItem("Features", "Lane Keep, Path Follow"),
                    InfoItem("Sensors", "GPS, IMU, Camera")
                )
            ),
            ConfigOption(
                id = "lane_keeping",
                name = "Lane Keeping Assist",
                description = "Keep cart centered in marked paths and lanes",
                currentValue = "Coming Soon",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "collision_warning",
                name = "Forward Collision Warning",
                description = "Alert when approaching obstacles too quickly",
                currentValue = "Coming Soon",
                controlType = ControlType.TOGGLE
            )
        )
    ),
    ConfigCategory(
        id = "locks",
        name = "Locks",
        icon = Icons.Default.Lock,
        options = listOf(
            ConfigOption(
                id = "cart_lock",
                name = "Lock Cart",
                description = "Enable or disable motor controller to prevent unauthorized use",
                currentValue = "Unlocked",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "speed_lock",
                name = "Speed Limit Lock",
                description = "Set and lock maximum speed to prevent tampering",
                currentValue = "Off",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "pin_to_drive",
                name = "PIN to Drive",
                description = "Require PIN entry before cart can be driven",
                currentValue = "Off",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "walk_away_lock",
                name = "Walk-Away Door Lock",
                description = "Automatically lock when you walk away with your phone",
                currentValue = "Off",
                controlType = ControlType.TOGGLE
            )
        )
    ),
    ConfigCategory(
        id = "lights",
        name = "Lights",
        icon = Icons.Default.Lightbulb,
        options = listOf(
            ConfigOption(
                id = "headlights",
                name = "Headlights",
                description = "Control front headlight operation mode",
                currentValue = "Auto",
                controlType = ControlType.SELECTOR,
                selectorOptions = listOf("Off", "Auto", "On")
            ),
            ConfigOption(
                id = "daytime_running",
                name = "Daytime Running Lights",
                description = "Keep lights on during daylight for visibility",
                currentValue = "On",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "dome_lights",
                name = "Interior Lights",
                description = "Control cabin and cargo area lighting",
                currentValue = "Auto",
                controlType = ControlType.SELECTOR,
                selectorOptions = listOf("Off", "Auto", "On")
            ),
            ConfigOption(
                id = "light_brightness",
                name = "Exterior Light Brightness",
                description = "Adjust brightness of all exterior lights",
                currentValue = "85",
                controlType = ControlType.SLIDER
            ),
            ConfigOption(
                id = "ambient_lights",
                name = "Ambient Lighting",
                description = "Control ambient interior lighting color and intensity",
                currentValue = "50",
                controlType = ControlType.SLIDER
            )
        )
    ),
    ConfigCategory(
        id = "display",
        name = "Display",
        icon = Icons.Default.PhoneAndroid,
        options = listOf(
            ConfigOption(
                id = "brightness",
                name = "Brightness",
                description = "Adjust screen brightness level",
                currentValue = "75",
                controlType = ControlType.SLIDER
            ),
            ConfigOption(
                id = "auto_brightness",
                name = "Auto Brightness",
                description = "Automatically adjust brightness based on ambient light",
                currentValue = "On",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "dark_mode",
                name = "Dark Mode",
                description = "Always use dark theme for reduced eye strain",
                currentValue = "Always On",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Current Theme", "Dark"),
                    InfoItem("Auto Switch", "Disabled")
                )
            ),
            ConfigOption(
                id = "screen_timeout",
                name = "Screen Timeout",
                description = "Time before screen dims when idle",
                currentValue = "Never",
                controlType = ControlType.SELECTOR,
                selectorOptions = listOf("1 min", "5 min", "10 min", "Never")
            ),
            ConfigOption(
                id = "touchscreen_sensitivity",
                name = "Touchscreen Sensitivity",
                description = "Adjust touch sensitivity for gloved or bare hand use",
                currentValue = "Standard",
                controlType = ControlType.SELECTOR,
                selectorOptions = listOf("Low", "Standard", "High")
            )
        )
    ),
    ConfigCategory(
        id = "navigation",
        name = "Navigation",
        icon = Icons.Default.Map,
        options = listOf(
            ConfigOption(
                id = "gps_location",
                name = "GPS & Location",
                description = "View detailed GPS status, satellite information, and location accuracy",
                currentValue = "Active",
                controlType = ControlType.GPS_DETAIL
            ),
            ConfigOption(
                id = "map_detail",
                name = "Map Details",
                description = "Show or hide points of interest and labels on map",
                currentValue = "Standard",
                controlType = ControlType.SELECTOR,
                selectorOptions = listOf("Minimal", "Standard", "Detailed")
            ),
            ConfigOption(
                id = "online_routing",
                name = "Online Routing",
                description = "Use internet for traffic-aware route calculation",
                currentValue = "On",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "auto_navigation",
                name = "Automatic Navigation",
                description = "Suggest routes based on calendar and frequent destinations",
                currentValue = "Off",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "voice_guidance",
                name = "Voice Guidance",
                description = "Spoken turn-by-turn navigation instructions",
                currentValue = "On",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "north_up",
                name = "Map Orientation",
                description = "Choose how the map rotates as you drive",
                currentValue = "Heading Up",
                controlType = ControlType.SELECTOR,
                selectorOptions = listOf("North Up", "Heading Up")
            )
        )
    ),
    ConfigCategory(
        id = "safety",
        name = "Safety & Security",
        icon = Icons.Default.Security,
        options = listOf(
            ConfigOption(
                id = "speed_limit",
                name = "Speed Limit",
                description = "Set maximum allowed speed for safety",
                currentValue = "25",
                controlType = ControlType.SLIDER
            ),
            ConfigOption(
                id = "speed_warning",
                name = "Speed Limit Warning",
                description = "Alert when exceeding set speed limit",
                currentValue = "On",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "seatbelt_reminder",
                name = "Seatbelt Reminder",
                description = "Alert if seatbelt not fastened while moving",
                currentValue = "On",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "parking_brake",
                name = "Parking Brake",
                description = "Automatically engage parking brake when stopped",
                currentValue = "Auto",
                controlType = ControlType.SELECTOR,
                selectorOptions = listOf("Off", "Auto", "Always")
            ),
            ConfigOption(
                id = "dashcam",
                name = "Dashcam",
                description = "Record video while driving using front camera",
                currentValue = "Off",
                controlType = ControlType.TOGGLE
            )
        )
    ),
    ConfigCategory(
        id = "service",
        name = "Service",
        icon = Icons.Default.Build,
        options = listOf(
            ConfigOption(
                id = "diagnostics",
                name = "System Diagnostics",
                description = "View real-time diagnostic information and error codes",
                currentValue = "View diagnostics",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("System Status", "OK"),
                    InfoItem("Error Codes", "None"),
                    InfoItem("Last Service", "Never"),
                    InfoItem("Odometer", "247 km")
                )
            ),
            ConfigOption(
                id = "can_monitor",
                name = "CAN Bus Monitor",
                description = "Real-time monitoring of CAN bus messages for debugging",
                currentValue = "Advanced feature",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("CAN Status", "Active"),
                    InfoItem("Bus Load", "23%"),
                    InfoItem("Error Frames", "0")
                )
            ),
            ConfigOption(
                id = "service_mode",
                name = "Service Mode",
                description = "Enable service mode for maintenance and diagnostics",
                currentValue = "Off",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "reset_trip",
                name = "Reset Trip Meter",
                description = "Reset trip distance and energy consumption",
                currentValue = "Reset now",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Trip Distance", "15.3 km"),
                    InfoItem("Trip Energy", "2.1 kWh"),
                    InfoItem("Avg Speed", "18 km/h")
                )
            )
        )
    ),
    ConfigCategory(
        id = "software",
        name = "Software",
        icon = Icons.Default.CloudDownload,
        options = listOf(
            ConfigOption(
                id = "version",
                name = "Software Version",
                description = "Current system software version and update status",
                currentValue = "v1.0.0",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Version", "1.0.0-alpha"),
                    InfoItem("Build Date", "2025-01-15"),
                    InfoItem("Updates", "Up to date")
                )
            ),
            ConfigOption(
                id = "auto_update",
                name = "Software Updates",
                description = "Automatically download and install updates",
                currentValue = "On",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "release_notes",
                name = "Release Notes",
                description = "View what's new in the latest software version",
                currentValue = "View notes",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Latest", "1.0.0-alpha"),
                    InfoItem("Features", "Initial release"),
                    InfoItem("Size", "124 MB")
                )
            ),
            ConfigOption(
                id = "factory_reset",
                name = "Factory Reset",
                description = "Restore all settings to factory defaults",
                currentValue = "Reset",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Warning", "This will erase all settings"),
                    InfoItem("Action", "Contact support")
                )
            )
        )
    ),
    ConfigCategory(
        id = "motors",
        name = "Motor Controller",
        icon = Icons.Default.ElectricBolt,
        options = listOf(
            ConfigOption(
                id = "kelly_status",
                name = "Kelly Controller Status",
                description = "Monitor Kelly motor controller health and performance",
                currentValue = "Healthy",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Controller Temp", "42°C"),
                    InfoItem("Motor Temp", "38°C"),
                    InfoItem("Throttle", "0%"),
                    InfoItem("RPM", "0")
                )
            ),
            ConfigOption(
                id = "motor_current_limit",
                name = "Motor Current Limit",
                description = "Set maximum motor current for performance control",
                currentValue = "80",
                controlType = ControlType.SLIDER
            ),
            ConfigOption(
                id = "field_weakening",
                name = "Field Weakening",
                description = "Enable higher top speed with field weakening",
                currentValue = "Off",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "motor_temperature_limit",
                name = "Temperature Protection",
                description = "Automatically reduce power if motor overheats",
                currentValue = "On",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "hall_sensor_test",
                name = "Hall Sensor Test",
                description = "Test motor hall sensors for proper operation",
                currentValue = "Run test",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Hall A", "OK"),
                    InfoItem("Hall B", "OK"),
                    InfoItem("Hall C", "OK")
                )
            )
        )
    ),
    ConfigCategory(
        id = "solar",
        name = "Solar Charging",
        icon = Icons.Default.WbSunny,
        options = listOf(
            ConfigOption(
                id = "mppt_status",
                name = "MPPT Status",
                description = "Maximum Power Point Tracking controller status and output",
                currentValue = "Tracking - 45W",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Output Power", "45W"),
                    InfoItem("Panel Voltage", "21.3V"),
                    InfoItem("Panel Current", "2.1A"),
                    InfoItem("Efficiency", "96%")
                )
            ),
            ConfigOption(
                id = "solar_priority",
                name = "Solar Priority",
                description = "Prioritize solar charging over grid charging when available",
                currentValue = "On",
                controlType = ControlType.TOGGLE
            ),
            ConfigOption(
                id = "mppt_voltage",
                name = "MPPT Voltage Setpoint",
                description = "Target voltage for maximum power point tracking",
                currentValue = "18",
                controlType = ControlType.SLIDER
            ),
            ConfigOption(
                id = "solar_stats",
                name = "Solar Statistics",
                description = "Lifetime solar charging statistics",
                currentValue = "View stats",
                controlType = ControlType.INFO,
                infoItems = listOf(
                    InfoItem("Total Energy", "134 kWh"),
                    InfoItem("CO2 Saved", "89 kg"),
                    InfoItem("Best Day", "3.2 kWh"),
                    InfoItem("Avg Daily", "1.8 kWh")
                )
            )
        )
    )
)
