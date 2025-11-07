package io.battlewithbytes.carlauncher.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.battlewithbytes.carlauncher.can.CanBusManager
import io.battlewithbytes.carlauncher.can.protocol.CanMessage
import io.battlewithbytes.carlauncher.can.sender
import io.battlewithbytes.carlauncher.can.subscribeToMessage

/**
 * Interactive widgets that both SUBSCRIBE and SEND CAN messages
 *
 * Demonstrates the full pub/sub cycle:
 * - Widget subscribes to status messages
 * - User interacts with widget
 * - Widget sends command messages
 * - Status updates come back via subscription
 */

/**
 * Eco Mode Toggle - subscribes to switch state, sends switch commands
 */
@Composable
fun EcoModeToggleWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    // Subscribe to current switch state
    val switchState = canBusManager.subscribeToMessage<CanMessage.SwitchState>()
    val isEcoMode = switchState?.eco ?: false

    Card(
        modifier = modifier.clickable {
            // Send command to toggle eco mode
            canBusManager.sender.sendSwitchCommand(
                eco = !isEcoMode,
                // Preserve other switch states
                brake = switchState?.brake ?: false,
                reverse = switchState?.reverse ?: false,
                foot = switchState?.foot ?: false,
                forward = switchState?.forward ?: false
            )
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isEcoMode) Color(0xFF4CAF50) else Color(0xFF1A1A1A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Eco Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = if (isEcoMode) "ACTIVE" else "Off",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF808080)
                )
            }
            Switch(
                checked = isEcoMode,
                onCheckedChange = null,  // Handled by card click
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2E7D32)
                )
            )
        }
    }
}

/**
 * Trip Reset Button - subscribes to trip stats, sends reset command
 */
@Composable
fun TripResetWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Trip Statistics",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3E6AE1)
                )
            ) {
                Text("Reset Trip")
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Reset Trip?") },
            text = { Text("This will reset trip distance, max speed, and average speed.") },
            confirmButton = {
                TextButton(onClick = {
                    canBusManager.sender.resetTripStatistics()
                    showConfirmDialog = false
                }) {
                    Text("Reset", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Diagnostic Request Widget - sends diagnostic queries, shows responses
 */
@Composable
fun DiagnosticWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    var selectedDevice by remember { mutableStateOf(0x601) }  // Motor Controller 1
    val devices = mapOf(
        0x600 to "Wiring Harness",
        0x601 to "Motor Controller 1",
        0x602 to "Motor Controller 2",
        0x604 to "BMS",
        0x605 to "Solar Controller",
        0x606 to "GPS Module",
        0x607 to "Battery Monitor"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Device selector
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3E6AE1)
                    )
                ) {
                    Text(devices[selectedDevice] ?: "Unknown")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    devices.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedDevice = id
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        canBusManager.sender.requestDiagnostics(selectedDevice, 0x01)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3E6AE1)
                    )
                ) {
                    Text("Query Status")
                }

                Button(
                    onClick = {
                        canBusManager.sender.requestDiagnostics(selectedDevice, 0x02)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3E6AE1)
                    )
                ) {
                    Text("Get Errors")
                }
            }
        }
    }
}

/**
 * Time Sync Widget - subscribes to network time, sends sync requests
 */
@Composable
fun TimeSyncWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Network Time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF808080)
                )
                Text(
                    text = "Tap to sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF606060)
                )
            }

            IconButton(
                onClick = {
                    canBusManager.sender.requestTimeSync()
                }
            ) {
                Text("🔄", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

/**
 * Custom Command Builder Widget - for testing/debugging
 */
@Composable
fun CustomCommandWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    var canId by remember { mutableStateOf("0x500") }
    var dataHex by remember { mutableStateOf("01,02,03,04,05,06,07") }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Command",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "▲" else "▼", color = Color.White)
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // CAN ID input
                OutlinedTextField(
                    value = canId,
                    onValueChange = { canId = it },
                    label = { Text("CAN ID (hex)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3E6AE1),
                        unfocusedBorderColor = Color(0xFF606060),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Data bytes input
                OutlinedTextField(
                    value = dataHex,
                    onValueChange = { dataHex = it },
                    label = { Text("Data (comma-separated hex)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3E6AE1),
                        unfocusedBorderColor = Color(0xFF606060),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        try {
                            val id = canId.removePrefix("0x").toInt(16)
                            val data = dataHex.split(",").map {
                                it.trim().toInt(16).toByte()
                            }.toByteArray()

                            canBusManager.sender.sendRawMessage(id, data)
                        } catch (e: Exception) {
                            // Show error
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3E6AE1)
                    )
                ) {
                    Text("Send Message")
                }
            }
        }
    }
}

/**
 * Complete interactive dashboard example
 */
@Composable
fun InteractiveDashboard(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Vehicle Controls",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        // Control widgets
        EcoModeToggleWidget(canBusManager, Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TripResetWidget(canBusManager, Modifier.weight(1f))
            TimeSyncWidget(canBusManager, Modifier.weight(1f))
        }

        DiagnosticWidget(canBusManager, Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Debug Tools",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF808080)
        )

        CustomCommandWidget(canBusManager, Modifier.fillMaxWidth())
    }
}
