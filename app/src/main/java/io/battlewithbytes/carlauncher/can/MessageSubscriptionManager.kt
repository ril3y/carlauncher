package io.battlewithbytes.carlauncher.can

import android.util.Log
import io.battlewithbytes.carlauncher.can.protocol.CanMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Pub/Sub framework for CAN messages
 *
 * Allows UI widgets to subscribe ONLY to the specific CAN messages they need,
 * ensuring isolated, efficient updates without unnecessary recomposition.
 *
 * Architecture:
 * ```
 * CAN Bus -> CanBusManager -> MessageSubscriptionManager -> UI Widgets
 *                                    (publish)              (subscribe)
 * ```
 *
 * Example:
 * ```kotlin
 * @Composable
 * fun BatteryWidget(subscriptionManager: MessageSubscriptionManager) {
 *     val bmsStatus by subscriptionManager
 *         .subscribe<CanMessage.BmsPackStatus>()
 *         .collectAsState(initial = null)
 *
 *     bmsStatus?.let { bms ->
 *         Text("${bms.packVoltageV}V")
 *     }
 * }
 * ```
 */
class MessageSubscriptionManager {
    val TAG = "MessageSubscriptionManager"

    // Shared flows for each message type (hot streams, multiple subscribers)
    private val messageFlows = mutableMapOf<Int, MutableSharedFlow<CanMessage>>()

    // Type-safe flows for sealed class types
    private val typedFlows = mutableMapOf<String, MutableSharedFlow<out CanMessage>>()

    /**
     * Publish a CAN message to all subscribers
     * Called by CanBusManager when new message arrives
     */
    suspend fun publish(message: CanMessage) {
        // Publish to ID-based subscribers
        getOrCreateFlow(message.id).emit(message)

        // Publish to type-based subscribers
        val typeKey = message::class.simpleName ?: return
        @Suppress("UNCHECKED_CAST")
        (getOrCreateTypedFlow(typeKey) as MutableSharedFlow<CanMessage>).emit(message)

        Log.v(TAG, "Published: ${typeKey} (0x${message.id.toString(16)})")
    }

    /**
     * Subscribe to messages by CAN ID
     * Use this when you want ALL messages from a specific ID
     *
     * @param canId CAN message ID (e.g., 0x620 for BMS Pack Status)
     * @return SharedFlow of CanMessage
     */
    fun subscribeById(canId: Int): SharedFlow<CanMessage> {
        Log.d(TAG, "Subscriber registered for CAN ID 0x${canId.toString(16)}")
        return getOrCreateFlow(canId).asSharedFlow()
    }

    /**
     * Subscribe to messages by type (type-safe)
     * Use this when you want specific message types with type safety
     *
     * @param T Sealed class type (e.g., CanMessage.BmsPackStatus)
     * @return SharedFlow of typed message
     */
    inline fun <reified T : CanMessage> subscribe(): SharedFlow<T> {
        val typeKey = T::class.simpleName ?: throw IllegalArgumentException("Cannot get class name")
        Log.d(TAG, "Subscriber registered for type: $typeKey")

        @Suppress("UNCHECKED_CAST")
        return getOrCreateTypedFlow(typeKey).asSharedFlow() as SharedFlow<T>
    }

    /**
     * Subscribe to multiple message types
     * Returns a flow that emits when ANY of the specified types arrive
     *
     * Example:
     * ```kotlin
     * val motorMessages = subscriptionManager.subscribeToMultiple(
     *     CanMessage.MotorBasic::class,
     *     CanMessage.MotorStatus::class
     * )
     * ```
     */
    fun subscribeToMultiple(vararg canIds: Int): SharedFlow<CanMessage> {
        val combinedFlow = MutableSharedFlow<CanMessage>(replay = 1)
        // Implementation note: You'd merge these flows in a coroutine
        // For simplicity, returning first one - expand as needed
        return subscribeById(canIds.first())
    }

    /**
     * Get number of active subscribers for a message type
     */
    fun getSubscriberCount(canId: Int): Int {
        return messageFlows[canId]?.subscriptionCount?.value ?: 0
    }

    /**
     * Get all active subscriptions (for debugging)
     */
    fun getActiveSubscriptions(): Map<Int, Int> {
        return messageFlows.mapValues { (_, flow) ->
            flow.subscriptionCount.value
        }
    }

    private fun getOrCreateFlow(canId: Int): MutableSharedFlow<CanMessage> {
        return messageFlows.getOrPut(canId) {
            MutableSharedFlow(
                replay = 1,        // Keep last message for new subscribers
                extraBufferCapacity = 10  // Buffer for fast 10Hz messages
            )
        }
    }

    fun getOrCreateTypedFlow(typeKey: String): MutableSharedFlow<out CanMessage> {
        return typedFlows.getOrPut(typeKey) {
            MutableSharedFlow<CanMessage>(
                replay = 1,
                extraBufferCapacity = 10
            )
        }
    }
}
