# Bidirectional CAN Widget Framework

## Overview

Complete framework for widgets that both **SUBSCRIBE** (receive) and **SEND** (transmit) CAN messages. This enables fully interactive vehicle control UI.

```
┌─────────────────────────────────────────────────────────┐
│                    CAN Bus Network                       │
│         (Golf Cart - 500kbps, 46 message types)         │
└──────────────┬───────────────────┬──────────────────────┘
               │                   │
          ┌────▼────┐         ┌────▼────┐
          │Subscribe│         │ Publish │
          │(Receive)│         │ (Send)  │
          └────┬────┘         └────┬────┘
               │                   │
┌──────────────▼───────────────────▼──────────────────────┐
│              CanBusManager                               │
│  ┌─────────────────────┐    ┌──────────────────────┐   │
│  │MessageSubscription  │    │CanMessageSender      │   │
│  │Manager              │    │                      │   │
│  │(Pub/Sub Hub)        │    │(CRC8 + Validation)   │   │
│  └─────────┬───────────┘    └──────────┬───────────┘   │
└────────────┼────────────────────────────┼───────────────┘
             │                            │
      ┌──────▼──────┐              ┌─────▼──────┐
      │  Subscribe  │              │    Send    │
      │   to State  │              │  Commands  │
      └──────┬──────┘              └─────┬──────┘
             │                            │
      ┌──────▼────────────────────────────▼──────┐
      │        Interactive Widget                 │
      │  - Displays current state (subscribed)    │
      │  - User clicks/taps                       │
      │  - Sends command to CAN bus               │
      │  - Receives updated state                 │
      └───────────────────────────────────────────┘
```

## Architecture: Request/Response Cycle

### Example: Eco Mode Toggle

```
┌─────────────────────────────────────────────────────────┐
│ User Taps "Eco Mode" Button                             │
└────────────────────┬────────────────────────────────────┘
                     │
         ┌───────────▼───────────┐
         │ Widget sends command  │
         │ 0x500: eco=true       │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │ CanMessageSender      │
         │ - Adds CRC8           │
         │ - Sends to USB bridge │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │ CAN Bus (500kbps)     │
         │ Message broadcast     │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │ Controller receives   │
         │ Sets eco mode         │
         │ Broadcasts new state  │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │ CanBusManager RX      │
         │ Parses 0x500 response │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │ MessageSubscription   │
         │ Publishes to widgets  │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │ Widget recomposes     │
         │ Shows eco=true ✓      │
         └───────────────────────┘
```

**Total round-trip time**: ~20-50ms (depends on controller response time)

## Sending Messages: Three Patterns

### Pattern 1: Helper Functions (Recommended)

```kotlin
@Composable
fun EcoModeToggle(canBusManager: CanBusManager) {
    // Subscribe to current state
    val switches = canBusManager.subscribeToMessage<CanMessage.SwitchState>()

    Button(
        onClick = {
            // Send command using helper
            canBusManager.sender.sendSwitchCommand(
                eco = !switches?.eco ?: false
            )
        }
    ) {
        Text(if (switches?.eco == true) "Eco: ON" else "Eco: OFF")
    }
}
```

**Pros:**
- Type-safe, validated
- Automatic CRC calculation
- Helper handles data formatting
- Less error-prone

### Pattern 2: Raw Message Builder

```kotlin
@Composable
fun CustomCommand(canBusManager: CanBusManager) {
    Button(onClick = {
        canBusManager.sender.buildMessage(0x500)
            .putByte(0x01)           // Bitmap: brake on
            .putByte(0x00)           // Sequence
            .putByte(0x00)           // Debounce
            .putByte(0x00)           // Counter
            .putByte(0x00)
            .putByte(0x00)
            .putByte(0x00)
            .build(canBusManager.sender)
    }) {
        Text("Send Custom")
    }
}
```

**Pros:**
- Full control
- Good for complex messages
- Chainable builder pattern

**Use when:** Implementing new message types not yet in helpers

### Pattern 3: Direct Raw Bytes

```kotlin
@Composable
fun DebugCommand(canBusManager: CanBusManager) {
    Button(onClick = {
        val data = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        canBusManager.sender.sendRawMessage(0x500, data)
    }) {
        Text("Send Raw")
    }
}
```

**Pros:**
- Maximum flexibility
- Direct byte control

**Use when:** Debugging, reverse engineering, testing

## Example: Complete Bidirectional Widget

### Eco Mode Toggle (Subscribe + Send)

```kotlin
@Composable
fun EcoModeWidget(canBusManager: CanBusManager) {
    // SUBSCRIBE: Get current eco mode state
    val switchState = canBusManager.subscribeToMessage<CanMessage.SwitchState>()
    val isEcoActive = switchState?.eco ?: false

    // DISPLAY: Show current state
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEcoActive) Color.Green else Color.Gray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // SEND: Toggle eco mode
                    canBusManager.sender.sendSwitchCommand(
                        eco = !isEcoActive,
                        // Preserve other states
                        brake = switchState?.brake ?: false,
                        reverse = switchState?.reverse ?: false,
                        foot = switchState?.foot ?: false,
                        forward = switchState?.forward ?: false
                    )
                }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Eco Mode", color = Color.White)
                Text(
                    if (isEcoActive) "ACTIVE" else "Off",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Switch(checked = isEcoActive, onCheckedChange = null)
        }
    }
}
```

**Flow:**
1. Widget subscribes to `0x500` (SwitchState)
2. Displays current eco mode status
3. User taps card
4. Sends new command via `sendSwitchCommand()`
5. Controller updates state
6. Widget receives update via subscription
7. UI recomposes with new state

## Interactive Widget Examples

### 1. Trip Reset

```kotlin
@Composable
fun TripResetWidget(canBusManager: CanBusManager) {
    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = true }) {
        Text("Reset Trip")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Reset Trip Statistics?") },
            confirmButton = {
                TextButton(onClick = {
                    canBusManager.sender.resetTripStatistics()
                    showDialog = false
                }) {
                    Text("Reset", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

### 2. Diagnostic Request

```kotlin
@Composable
fun DiagnosticWidget(canBusManager: CanBusManager) {
    val devices = mapOf(
        0x601 to "Motor Controller 1",
        0x602 to "Motor Controller 2",
        0x604 to "BMS"
    )
    var selectedDevice by remember { mutableStateOf(0x601) }

    Column {
        Text("Request Diagnostics")

        devices.forEach { (id, name) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        canBusManager.sender.requestDiagnostics(id, 0x01)
                    }
                    .padding(12.dp)
            ) {
                Text(name)
            }
        }
    }
}
```

### 3. Time Sync Button

```kotlin
@Composable
fun TimeSyncWidget(canBusManager: CanBusManager) {
    IconButton(
        onClick = { canBusManager.sender.requestTimeSync() }
    ) {
        Icon(Icons.Default.Sync, "Sync Time")
    }
}
```

## CAN Message Sender API Reference

### Built-in Helpers

```kotlin
// Switch control (0x500)
sendSwitchCommand(
    brake: Boolean = false,
    eco: Boolean = false,
    reverse: Boolean = false,
    foot: Boolean = false,
    forward: Boolean = false
)

// Diagnostic request (0x700)
requestDiagnostics(
    deviceId: Int,
    diagnosticCode: Int
)

// Time sync (0x644)
requestTimeSync()

// Trip reset (0x649)
resetTripStatistics()

// Raw message (any ID)
sendRawMessage(canId: Int, dataBytes: ByteArray)
```

### Message Builder

```kotlin
canBusManager.sender.buildMessage(0x500)
    .putByte(0x01)              // 8-bit signed
    .putUByte(255u)             // 8-bit unsigned
    .putShort(1000)             // 16-bit signed
    .putUShort(5000u)           // 16-bit unsigned
    .putInt(100000)             // 32-bit signed
    .putFloat(52.5f, 0.1f)      // Float with scale factor
    .build(canBusManager.sender)
```

## Safety & Validation

### Automatic CRC8

All sent messages automatically include CRC8 checksum:

```kotlin
// You provide 7 bytes
val data = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)

// Sender adds CRC8 as byte 8
canBusManager.sender.sendRawMessage(0x500, data)
// Actual sent: [01 02 03 04 05 06 07 XX] where XX = CRC8
```

### Data Validation

```kotlin
// Length validation
sendRawMessage(0x500, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))  // ❌ Error: Too long
sendRawMessage(0x500, byteArrayOf(1, 2, 3))                  // ✓ OK: Padded to 7 bytes

// Automatic padding
sendRawMessage(0x500, byteArrayOf(0x01))
// Sent as: [01 00 00 00 00 00 00 CRC]
```

### Logging

All sent messages are logged:

```
D/CanMessageSender: Sending CAN ID 0x500: 01,00,00,00,00,00,00,A3
I/CanMessageSender: Sent switch command: brake=false eco=true reverse=false
```

## Testing Interactive Widgets

### Mock Controller Responses

```kotlin
// In development, you can simulate controller responses
@Composable
fun MockResponseHandler(canBusManager: CanBusManager) {
    LaunchedEffect(Unit) {
        // Listen for outgoing commands
        // Simulate controller response after 50ms
        delay(50)

        // Inject mock response
        val mockSwitchState = byteArrayOf(
            0x02,  // Eco mode ON
            0x01,  // Sequence
            0x00, 0x00, 0x00, 0x00, 0x00
        )
        // (Would need access to inject into subscription manager)
    }
}
```

### Unit Test Example

```kotlin
@Test
fun testEcoModeToggle() = runTest {
    val canBusManager = CanBusManager(context)

    // Send eco mode command
    canBusManager.sender.sendSwitchCommand(eco = true)

    // Wait for response
    val switches = canBusManager.subscriptions
        .subscribe<CanMessage.SwitchState>()
        .first()

    assertEquals(true, switches.eco)
}
```

## Advanced: Custom Command Messages

### Implementing New Message Type

```kotlin
// 1. Add to CanMessageSender.kt
fun sendCustomCommand(param1: Int, param2: Float) {
    buildMessage(0x710)
        .putUShort(param1.toUShort())
        .putFloat(param2, 0.1f)
        .putByte(0x00)
        .putByte(0x00)
        .putByte(0x00)
        .build(this)

    Log.i(TAG, "Sent custom command: p1=$param1 p2=$param2")
}

// 2. Use in widget
@Composable
fun CustomControlWidget(canBusManager: CanBusManager) {
    Button(onClick = {
        canBusManager.sender.sendCustomCommand(1000, 52.5f)
    }) {
        Text("Send Custom")
    }
}
```

## Complete Example: Motor Control Panel

```kotlin
@Composable
fun MotorControlPanel(canBusManager: CanBusManager) {
    // SUBSCRIBE to motor telemetry
    val motor1 = canBusManager.subscribeToMessage<CanMessage.MotorBasic>()
        ?.takeIf { it.motorNumber == 1 }

    val motor1Status = canBusManager.subscribeToMessage<CanMessage.MotorStatus>()
        ?.takeIf { it.motorNumber == 1 }

    Column(modifier = Modifier.padding(16.dp)) {
        // DISPLAY current state
        Text("Motor 1 Status", style = MaterialTheme.typography.titleLarge)

        motor1?.let { m ->
            Text("RPM: ${m.rpm}")
            Text("Current: ${m.currentA}A")
            Text("Throttle: ${m.throttlePercent}%")
        }

        motor1Status?.let { s ->
            Text("Faults: ${if (s.hasFault) "YES" else "None"}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CONTROLS - Send commands
        Button(onClick = {
            canBusManager.sender.requestDiagnostics(0x601, 0x01)
        }) {
            Text("Request Motor 1 Status")
        }

        Button(onClick = {
            canBusManager.sender.requestDiagnostics(0x601, 0x02)
        }) {
            Text("Get Error Codes")
        }
    }
}
```

## Summary

The bidirectional framework provides:

✅ **Subscribe Pattern** - Widgets receive real-time updates
✅ **Send Pattern** - Widgets can send commands
✅ **Automatic CRC** - All messages validated
✅ **Type Safety** - Compile-time message checking
✅ **Helper Functions** - Common commands pre-built
✅ **Builder API** - Custom message construction
✅ **Logging** - All TX/RX logged for debugging
✅ **Validation** - Length and format checking

**Key Principle:** Each widget is isolated - it subscribes only to what it needs and sends only what it controls. This creates clean, testable, performant UI components.
