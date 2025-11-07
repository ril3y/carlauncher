package io.battlewithbytes.carlauncher.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.battlewithbytes.carlauncher.can.CanBusManager
import io.battlewithbytes.carlauncher.can.protocol.CanMessage
import io.battlewithbytes.carlauncher.can.subscribeToMessage
import io.battlewithbytes.carlauncher.can.subscribeAndMap

/**
 * Example isolated widgets using the CAN subscriber framework
 *
 * Each widget subscribes ONLY to the messages it needs, ensuring:
 * - No unnecessary recomposition
 * - Clear separation of concerns
 * - Easy to add/remove widgets without affecting others
 */

/**
 * Battery voltage display - subscribes ONLY to BMS Pack Status
 * Only recomposes when 0x620 messages arrive
 */
@Composable
fun BatteryVoltageWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    // This widget ONLY subscribes to BmsPackStatus messages
    val bmsStatus = canBusManager.subscribeToMessage<CanMessage.BmsPackStatus>()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pack Voltage",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF808080)
            )

            bmsStatus?.let { bms ->
                Text(
                    text = String.format("%.1f V", bms.packVoltageV),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF3E6AE1)
                )
                Text(
                    text = if (bms.crcValid) "Valid" else "CRC Error",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bms.crcValid) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            } ?: Text(
                text = "No Data",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF606060)
            )
        }
    }
}

/**
 * SOC display - subscribes ONLY to authoritative SOC message
 * Only recomposes when 0x651 messages arrive
 */
@Composable
fun SocWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    // Subscribes to AUTHORITATIVE SOC only
    val socData = canBusManager.subscribeToMessage<CanMessage.SocCapacity>()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "State of Charge",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF808080)
            )

            socData?.let { soc ->
                Text(
                    text = "${soc.soc}%",
                    style = MaterialTheme.typography.displayLarge,
                    color = when {
                        soc.soc > 60 -> Color(0xFF4CAF50)  // Green
                        soc.soc > 20 -> Color(0xFFFFC107)  // Yellow
                        else -> Color(0xFFF44336)           // Red
                    }
                )
                Text(
                    text = "Confidence: ${soc.socConfidence}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF808080)
                )
                Text(
                    text = String.format("%.1f Ah / %.1f Ah",
                        soc.remainingCapacityAh, soc.fullCapacityAh),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF606060)
                )
            } ?: Text(
                text = "No Data",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF606060)
            )
        }
    }
}

/**
 * Motor RPM gauge - subscribes to specific motor telemetry
 * Only recomposes when motor messages arrive
 */
@Composable
fun MotorRpmWidget(
    canBusManager: CanBusManager,
    motorNumber: Int = 1,
    modifier: Modifier = Modifier
) {
    // Subscribes to motor telemetry and filters by motor number
    val motorData = canBusManager.subscribeToMessage<CanMessage.MotorBasic>()
    val relevantMotor = if (motorData?.motorNumber == motorNumber) motorData else null

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Motor $motorNumber RPM",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF808080)
            )

            relevantMotor?.let { motor ->
                Text(
                    text = "${motor.rpm}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color(0xFF3E6AE1)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current", color = Color(0xFF606060), style = MaterialTheme.typography.labelSmall)
                        Text("${String.format("%.1f", motor.currentA)}A", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Throttle", color = Color(0xFF606060), style = MaterialTheme.typography.labelSmall)
                        Text("${motor.throttlePercent}%", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Temp", color = Color(0xFF606060), style = MaterialTheme.typography.labelSmall)
                        Text("${motor.motorTempC}°C", color = Color.White)
                    }
                }
            } ?: Text(
                text = "No Data",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF606060)
            )
        }
    }
}

/**
 * GPS speed display - subscribes ONLY to GPS velocity
 * Only recomposes when GPS velocity updates
 */
@Composable
fun SpeedWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    val gpsVelocity = canBusManager.subscribeToMessage<CanMessage.GpsVelocity>()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            gpsVelocity?.let { gps ->
                Text(
                    text = String.format("%.0f", gps.speedKmh),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color(0xFF3E6AE1)
                )
                Text(
                    text = "km/h",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF808080)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Course: ${String.format("%.0f", gps.courseDeg)}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF606060)
                )
            } ?: Text(
                text = "GPS No Fix",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF606060)
            )
        }
    }
}

/**
 * Power meter - subscribes to battery monitor for AUTHORITATIVE current
 * Only recomposes when power data updates
 */
@Composable
fun PowerMeterWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    val powerData = canBusManager.subscribeToMessage<CanMessage.PackCurrentPower>()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pack Power",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF808080)
            )

            powerData?.let { power ->
                Text(
                    text = String.format("%.2f kW", power.instantPowerW / 1000f),
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (power.packCurrentA > 0) Color(0xFFFFC107) else Color(0xFF4CAF50)
                )
                Text(
                    text = if (power.packCurrentA > 0) "Discharging" else "Charging",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF808080)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current", color = Color(0xFF606060), style = MaterialTheme.typography.labelSmall)
                        Text("${String.format("%.1f", power.packCurrentA)}A", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Voltage", color = Color(0xFF606060), style = MaterialTheme.typography.labelSmall)
                        Text("${String.format("%.1f", power.packVoltageV)}V", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Load", color = Color(0xFF606060), style = MaterialTheme.typography.labelSmall)
                        Text("${power.loadPercent}%", color = Color.White)
                    }
                }
            } ?: Text(
                text = "No Data",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF606060)
            )
        }
    }
}

/**
 * Critical alarm monitor - subscribes to BMS alarms
 * Shows alert when any critical alarms present
 */
@Composable
fun CriticalAlarmWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    val alarms = canBusManager.subscribeToMessage<CanMessage.BmsCriticalAlarms>()

    // Only show if alarms present
    if (alarms?.hasAnyAlarm == true) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "⚠️ CRITICAL ALARMS",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (alarms.voltageAlarms.toInt() != 0) {
                    Text("• Voltage Alarm", color = Color.White)
                }
                if (alarms.currentAlarms.toInt() != 0) {
                    Text("• Current Alarm", color = Color.White)
                }
                if (alarms.tempAlarms.toInt() != 0) {
                    Text("• Temperature Alarm", color = Color.White)
                }
                if (alarms.systemAlarms.toInt() != 0) {
                    Text("• System Alarm", color = Color.White)
                }
                if (alarms.mosfetFailures.toInt() != 0) {
                    Text("• MOSFET Failure", color = Color.White)
                }
            }
        }
    }
}

/**
 * Heartbeat monitor - shows health of all CAN devices
 */
@Composable
fun HeartbeatMonitorWidget(
    canBusManager: CanBusManager,
    modifier: Modifier = Modifier
) {
    // Subscribe to all heartbeat messages
    val heartbeat = canBusManager.subscribeToMessage<CanMessage.Heartbeat>()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Device Health",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            heartbeat?.let { hb ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = hb.deviceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF808080)
                    )
                    Text(
                        text = if (hb.isHealthy) "✓ OK" else "✗ ERROR",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hb.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
                if (hb.currentError.toInt() != 0) {
                    Text(
                        text = "Error Code: 0x${hb.currentError.toString(16)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF44336)
                    )
                }
            } ?: Text(
                text = "No heartbeats received",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF606060)
            )
        }
    }
}

/**
 * Example composite dashboard using isolated subscriber widgets
 */
@Composable
fun SubscriberDashboardExample(
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
        // Critical alarms always on top
        CriticalAlarmWidget(canBusManager)

        // Speed (large, prominent)
        SpeedWidget(canBusManager, Modifier.fillMaxWidth())

        // Battery info row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SocWidget(canBusManager, Modifier.weight(1f))
            BatteryVoltageWidget(canBusManager, Modifier.weight(1f))
        }

        // Power meter
        PowerMeterWidget(canBusManager, Modifier.fillMaxWidth())

        // Motors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MotorRpmWidget(canBusManager, motorNumber = 1, Modifier.weight(1f))
            MotorRpmWidget(canBusManager, motorNumber = 2, Modifier.weight(1f))
        }

        // Device health
        HeartbeatMonitorWidget(canBusManager, Modifier.fillMaxWidth())
    }
}
