package io.battlewithbytes.carlauncher.can.messages

import io.battlewithbytes.carlauncher.can.*

/**
 * Switch Control Messages (0x500 range)
 * Event-driven + 1Hz keepalive, critical priority
 */

/**
 * 0x500 - Switch State Message
 * Transmission: Event-driven + 1Hz keepalive
 */
data class SwitchStateMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Switch states (Byte 0 bitfield)
    val brakePressed: Boolean,      // Bit 0
    val ecoModeEnabled: Boolean,    // Bit 1
    val reverseMode: Boolean,       // Bit 2
    val footThrottle: Boolean,      // Bit 3
    val forwardMode: Boolean,       // Bit 4

    // Sequence counter (Byte 1)
    val sequenceCounter: UByte,     // 0-255 rolling

    // Debounce status (Byte 2 bitfield)
    val brakeStable: Boolean,       // Bit 0
    val ecoStable: Boolean,         // Bit 1
    val reverseStable: Boolean,     // Bit 2
    val footStable: Boolean,        // Bit 3
    val forwardStable: Boolean,     // Bit 4

    // State change counter (Byte 3)
    val stateChangeCounter: UByte   // Increments on any state change

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x500
        const val UPDATE_RATE_MS = 1000L // 1Hz keepalive
        const val MAX_AGE_MS = 2000L // Stale after 2 seconds

        /**
         * Parses a CAN frame into a SwitchStateMessage
         */
        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<SwitchStateMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val switchStates = data.getUInt8(0)
            val sequenceCounter = data.getUInt8(1)
            val debounceStatus = data.getUInt8(2)
            val stateChangeCounter = data.getUInt8(3)

            val message = SwitchStateMessage(
                timestamp = timestamp,
                isValid = true,
                brakePressed = switchStates.isBitSet(0),
                ecoModeEnabled = switchStates.isBitSet(1),
                reverseMode = switchStates.isBitSet(2),
                footThrottle = switchStates.isBitSet(3),
                forwardMode = switchStates.isBitSet(4),
                sequenceCounter = sequenceCounter,
                brakeStable = debounceStatus.isBitSet(0),
                ecoStable = debounceStatus.isBitSet(1),
                reverseStable = debounceStatus.isBitSet(2),
                footStable = debounceStatus.isBitSet(3),
                forwardStable = debounceStatus.isBitSet(4),
                stateChangeCounter = stateChangeCounter
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if all switches have stable readings
     */
    fun allStable(): Boolean {
        return brakeStable && ecoStable && reverseStable && footStable && forwardStable
    }

    /**
     * Checks if any switch is currently bouncing
     */
    fun hasBouncingSwitch(): Boolean = !allStable()

    /**
     * Gets the current driving mode
     */
    fun getDrivingMode(): DrivingMode {
        return when {
            forwardMode && !reverseMode -> DrivingMode.FORWARD
            reverseMode && !forwardMode -> DrivingMode.REVERSE
            !forwardMode && !reverseMode -> DrivingMode.NEUTRAL
            else -> DrivingMode.CONFLICT // Both forward and reverse active
        }
    }

    /**
     * Checks if vehicle is in a safe state for operation
     */
    fun isSafeForOperation(): Boolean {
        return allStable() && getDrivingMode() != DrivingMode.CONFLICT
    }
}

/**
 * Driving mode enumeration
 */
enum class DrivingMode {
    NEUTRAL,
    FORWARD,
    REVERSE,
    CONFLICT  // Error state: both forward and reverse active
}
