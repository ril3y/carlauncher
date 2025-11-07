package io.battlewithbytes.carlauncher.can

import android.util.Log
import io.battlewithbytes.carlauncher.can.protocol.Crc8Calculator
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Helper for building and sending CAN messages from UI widgets
 *
 * Provides type-safe builders for common command messages
 */
class CanMessageSender(private val canBusManager: CanBusManager) {
    private val TAG = "CanMessageSender"

    /**
     * Send a raw CAN message with automatic CRC calculation
     *
     * @param canId CAN message ID
     * @param dataBytes Data bytes (7 bytes max, CRC will be added)
     */
    fun sendRawMessage(canId: Int, dataBytes: ByteArray) {
        require(dataBytes.size <= 7) { "Data must be 7 bytes or less (CRC is byte 8)" }

        // Pad to 7 bytes if needed
        val paddedData = ByteArray(7)
        dataBytes.copyInto(paddedData, 0, 0, dataBytes.size)

        // Calculate and append CRC
        val crc = Crc8Calculator.calculate(paddedData + byteArrayOf(0))
        val fullMessage = paddedData + byteArrayOf(crc)

        Log.d(TAG, "Sending CAN ID 0x${canId.toString(16)}: ${fullMessage.joinToString(",") { "%02X".format(it) }}")
        canBusManager.sendMessage(canId, fullMessage)
    }

    /**
     * Send a switch control command (0x500)
     * Used to remotely trigger switch states
     */
    fun sendSwitchCommand(
        brake: Boolean = false,
        eco: Boolean = false,
        reverse: Boolean = false,
        foot: Boolean = false,
        forward: Boolean = false
    ) {
        var bitmap = 0
        if (brake) bitmap = bitmap or 0x01
        if (eco) bitmap = bitmap or 0x02
        if (reverse) bitmap = bitmap or 0x04
        if (foot) bitmap = bitmap or 0x08
        if (forward) bitmap = bitmap or 0x10

        val data = byteArrayOf(
            bitmap.toByte(),
            0x00,  // Sequence (will be managed by controller)
            0x00,  // Debounce status
            0x00,  // Change counter
            0x00, 0x00, 0x00  // Reserved
        )

        sendRawMessage(0x500, data)
        Log.i(TAG, "Sent switch command: brake=$brake eco=$eco reverse=$reverse")
    }

    /**
     * Request diagnostic data from a device
     * Uses message ID 0x700 (diagnostic request)
     */
    fun requestDiagnostics(deviceId: Int, diagnosticCode: Int) {
        val data = byteArrayOf(
            deviceId.toByte(),
            diagnosticCode.toByte(),
            0x00, 0x00, 0x00, 0x00, 0x00
        )
        sendRawMessage(0x700, data)
        Log.i(TAG, "Requested diagnostics from device $deviceId, code $diagnosticCode")
    }

    /**
     * Send time synchronization request
     * Triggers network time broadcast from time authority
     */
    fun requestTimeSync() {
        sendRawMessage(0x644, ByteArray(7))  // Time query request
        Log.i(TAG, "Sent time sync request")
    }

    /**
     * Reset trip statistics on GPS module
     * Sends command to 0x649 handler
     */
    fun resetTripStatistics() {
        val data = byteArrayOf(
            0xFF.toByte(),  // Reset command
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )
        sendRawMessage(0x649, data)
        Log.i(TAG, "Sent trip reset command")
    }

    /**
     * Builder class for complex messages
     */
    class MessageBuilder(private val canId: Int) {
        private val buffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN)

        fun putByte(value: Byte) = apply { buffer.put(value) }
        fun putUByte(value: UByte) = apply { buffer.put(value.toByte()) }
        fun putShort(value: Short) = apply { buffer.putShort(value) }
        fun putUShort(value: UShort) = apply { buffer.putShort(value.toShort()) }
        fun putInt(value: Int) = apply { buffer.putInt(value) }
        fun putFloat(value: Float, scale: Float) = apply {
            buffer.putShort((value / scale).toInt().toShort())
        }

        fun build(sender: CanMessageSender) {
            val data = ByteArray(7)
            buffer.position(0)
            buffer.get(data)
            sender.sendRawMessage(canId, data)
        }
    }

    /**
     * Create a message builder for custom messages
     */
    fun buildMessage(canId: Int): MessageBuilder {
        return MessageBuilder(canId)
    }
}

/**
 * Extension functions for CanBusManager to make sending easy
 */

/**
 * Get message sender instance
 */
val CanBusManager.sender: CanMessageSender
    get() = CanMessageSender(this)

/**
 * Quick send extension
 */
fun CanBusManager.sendCommand(canId: Int, vararg bytes: Byte) {
    sender.sendRawMessage(canId, bytes)
}
