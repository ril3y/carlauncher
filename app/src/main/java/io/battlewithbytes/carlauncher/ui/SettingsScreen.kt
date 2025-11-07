package io.battlewithbytes.carlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tesla-style settings overlay
 */
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 100) { // Swipe down threshold
                            onDismiss()
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 0) { // Only track downward drags
                            dragOffset += dragAmount
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with swipe indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Swipe indicator bar
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(Color(0xFF404040))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Controls",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Settings grid (Tesla style)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(settingsItems) { item ->
                    SettingsCard(item = item)
                }
            }
        }
    }
}

@Composable
fun SettingsCard(item: SettingsItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { item.onClick() },
        color = Color(0xFF1A1A1A),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (item.isActive) Color(0xFF3E6AE1) else Color(0xFF808080),
                    modifier = Modifier.size(28.dp)
                )
                if (item.hasToggle) {
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = { item.onClick() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3E6AE1),
                            uncheckedThumbColor = Color(0xFF606060),
                            uncheckedTrackColor = Color(0xFF2A2A2A)
                        )
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF808080)
                )
            }
        }
    }
}

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val hasToggle: Boolean = false,
    val isActive: Boolean = false,
    val onClick: () -> Unit
)

// Settings items for golf cart
val settingsItems = listOf(
    SettingsItem(
        icon = Icons.Default.Lock,
        title = "Locks",
        subtitle = "Lock/Unlock",
        hasToggle = true,
        isActive = true,
        onClick = { /* TODO: Handle lock toggle */ }
    ),
    SettingsItem(
        icon = Icons.Default.AcUnit,
        title = "Climate",
        subtitle = "Temperature control",
        onClick = { /* TODO: Open climate controls */ }
    ),
    SettingsItem(
        icon = Icons.Default.BatteryChargingFull,
        title = "Charging",
        subtitle = "View charging status",
        onClick = { /* TODO: Open charging screen */ }
    ),
    SettingsItem(
        icon = Icons.Default.Lightbulb,
        title = "Lights",
        subtitle = "Headlights & signals",
        hasToggle = true,
        isActive = false,
        onClick = { /* TODO: Handle lights toggle */ }
    ),
    SettingsItem(
        icon = Icons.Default.Navigation,
        title = "Navigation",
        subtitle = "Maps & routing",
        onClick = { /* TODO: Open navigation settings */ }
    ),
    SettingsItem(
        icon = Icons.Default.Headphones,
        title = "Media Apps",
        subtitle = "Spotify, YouTube Music",
        onClick = { /* TODO: Open media app selector */ }
    ),
    SettingsItem(
        icon = Icons.Default.Bluetooth,
        title = "Bluetooth",
        subtitle = "Connect devices",
        hasToggle = true,
        isActive = true,
        onClick = { /* TODO: Handle Bluetooth toggle */ }
    ),
    SettingsItem(
        icon = Icons.Default.Wifi,
        title = "Wi-Fi",
        subtitle = "Network settings",
        onClick = { /* TODO: Open WiFi settings */ }
    ),
    SettingsItem(
        icon = Icons.Default.Speed,
        title = "Display",
        subtitle = "Brightness & theme",
        onClick = { /* TODO: Open display settings */ }
    ),
    SettingsItem(
        icon = Icons.Default.Settings,
        title = "System",
        subtitle = "About & updates",
        onClick = { /* TODO: Open system settings */ }
    )
)
