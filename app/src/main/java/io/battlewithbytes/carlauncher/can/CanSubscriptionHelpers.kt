package io.battlewithbytes.carlauncher.can

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import io.battlewithbytes.carlauncher.can.protocol.CanMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Composable extension functions for easy CAN message subscription
 *
 * These make it trivial for UI widgets to subscribe to exactly the messages they need
 */

/**
 * Subscribe to a specific CAN message type with automatic state handling
 *
 * This widget will ONLY recompose when messages of type T arrive.
 *
 * Example:
 * ```kotlin
 * @Composable
 * fun BatteryWidget(canBusManager: CanBusManager) {
 *     val bmsStatus = canBusManager.subscribeToMessage<CanMessage.BmsPackStatus>()
 *
 *     bmsStatus?.let { bms ->
 *         Text("Voltage: ${bms.packVoltageV}V")
 *         Text("Current: ${bms.packCurrentA}A")
 *     }
 * }
 * ```
 */
@Composable
inline fun <reified T : CanMessage> CanBusManager.subscribeToMessage(
    context: CoroutineContext = EmptyCoroutineContext
): T? {
    val flow = remember(subscriptions) {
        subscriptions.subscribe<T>()
    }
    return flow.collectAsState(initial = null, context = context).value
}

/**
 * Subscribe to a message by CAN ID (less type-safe but more flexible)
 *
 * Example:
 * ```kotlin
 * @Composable
 * fun GenericMessageDisplay(canBusManager: CanBusManager) {
 *     val message = canBusManager.subscribeToMessageById(0x620)
 *
 *     message?.let { msg ->
 *         when (msg) {
 *             is CanMessage.BmsPackStatus -> Text("BMS: ${msg.packVoltageV}V")
 *             else -> Text("Unknown message type")
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun CanBusManager.subscribeToMessageById(
    canId: Int,
    context: CoroutineContext = EmptyCoroutineContext
): CanMessage? {
    val flow = remember(subscriptions, canId) {
        subscriptions.subscribeById(canId)
    }
    return flow.collectAsState(initial = null, context = context).value
}

/**
 * Subscribe and map to a derived value
 * Widget only recomposes when the MAPPED value changes
 *
 * Example:
 * ```kotlin
 * @Composable
 * fun SocDisplay(canBusManager: CanBusManager) {
 *     val soc = canBusManager.subscribeAndMap<CanMessage.SocCapacity, Int?> { msg ->
 *         msg?.soc
 *     }
 *
 *     soc?.let { Text("SOC: $it%") }
 * }
 * ```
 */
@Composable
inline fun <reified T : CanMessage, R> CanBusManager.subscribeAndMap(
    crossinline transform: @DisallowComposableCalls (T?) -> R,
    context: CoroutineContext = EmptyCoroutineContext
): R {
    val message = subscribeToMessage<T>(context)
    return remember(message) {
        transform(message)
    }
}

/**
 * Extension to get SharedFlow directly (for advanced use cases)
 *
 * Example:
 * ```kotlin
 * val bmsFlow: SharedFlow<CanMessage.BmsPackStatus> =
 *     canBusManager.messageFlow()
 *
 * LaunchedEffect(Unit) {
 *     bmsFlow
 *         .filter { it.crcValid }
 *         .collect { bms ->
 *             // Custom processing
 *         }
 * }
 * ```
 */
inline fun <reified T : CanMessage> CanBusManager.messageFlow(): SharedFlow<T> {
    return subscriptions.subscribe<T>()
}

/**
 * Composable that executes a side effect when a specific message arrives
 *
 * Example:
 * ```kotlin
 * canBusManager.onMessage<CanMessage.BmsCriticalAlarms> { alarms ->
 *     if (alarms.hasAnyAlarm) {
 *         // Show alert dialog
 *         showCriticalAlarmDialog()
 *     }
 * }
 * ```
 */
@Composable
inline fun <reified T : CanMessage> CanBusManager.onMessage(
    crossinline action: (T) -> Unit
) {
    val message = subscribeToMessage<T>()
    androidx.compose.runtime.LaunchedEffect(message) {
        message?.let { action(it) }
    }
}
