package io.battlewithbytes.carlauncher.can

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * CAN protocol utilities for Golf Cart Vehicle Control System
 * Protocol: 500 kbps, 11-bit IDs, 8 bytes (7 data + 1 CRC8)
 */

/**
 * CRC8 calculation with polynomial 0x07, init 0x00
 * Processes bytes 0-6, returns checksum for byte 7
 */
fun ByteArray.calculateCrc8(): UByte {
    require(size >= 7) { "CAN frame must have at least 7 data bytes" }

    var crc: UByte = 0x00u

    for (i in 0 until 7) {
        crc = crc xor this[i].toUByte()

        repeat(8) {
            crc = if ((crc.toInt() and 0x80) != 0) {
                ((crc.toInt() shl 1) xor 0x07).toUByte()
            } else {
                (crc.toInt() shl 1).toUByte()
            }
        }
    }

    return crc
}

/**
 * Validates CRC8 checksum on a CAN frame
 * @return true if checksum is valid, false otherwise
 */
fun ByteArray.validateCrc8(): Boolean {
    require(size == 8) { "CAN frame must be exactly 8 bytes" }
    return calculateCrc8() == this[7].toUByte()
}

/**
 * Base interface for all CAN messages
 */
interface CanMessage {
    val canId: Int
    val timestamp: Long
    val isValid: Boolean

    /**
     * Gets the staleness of this message in milliseconds
     */
    fun getStaleness(): Long = System.currentTimeMillis() - timestamp

    /**
     * Checks if message is stale based on expected update rate
     * @param maxAgeMs Maximum age in milliseconds before considered stale
     */
    fun isStale(maxAgeMs: Long): Boolean = getStaleness() > maxAgeMs
}

/**
 * Result type for CAN message parsing
 */
sealed class ParseResult<out T : CanMessage> {
    data class Success<T : CanMessage>(val message: T) : ParseResult<T>()
    data class InvalidCrc(val canId: Int, val rawData: ByteArray) : ParseResult<Nothing>()
    data class InvalidData(val canId: Int, val reason: String) : ParseResult<Nothing>()
}

/**
 * Extension functions for ByteArray parsing
 */

fun ByteArray.getUInt8(offset: Int): UByte = this[offset].toUByte()

fun ByteArray.getInt8(offset: Int): Byte = this[offset]

fun ByteArray.getUInt16LE(offset: Int): UShort {
    return ((this[offset].toUByte().toInt() or
             (this[offset + 1].toUByte().toInt() shl 8))).toUShort()
}

fun ByteArray.getInt16LE(offset: Int): Short {
    return (this[offset].toUByte().toInt() or
            (this[offset + 1].toUByte().toInt() shl 8)).toShort()
}

fun ByteArray.getUInt24LE(offset: Int): Int {
    return this[offset].toUByte().toInt() or
           (this[offset + 1].toUByte().toInt() shl 8) or
           (this[offset + 2].toUByte().toInt() shl 16)
}

fun ByteArray.getInt24LE(offset: Int): Int {
    val value = this[offset].toUByte().toInt() or
                (this[offset + 1].toUByte().toInt() shl 8) or
                (this[offset + 2].toUByte().toInt() shl 16)
    // Sign extend if bit 23 is set
    return if ((value and 0x800000) != 0) {
        value or 0xFF000000.toInt()
    } else {
        value
    }
}

fun ByteArray.getUInt32LE(offset: Int): UInt {
    return (this[offset].toUByte().toUInt() or
            (this[offset + 1].toUByte().toUInt() shl 8) or
            (this[offset + 2].toUByte().toUInt() shl 16) or
            (this[offset + 3].toUByte().toUInt() shl 24))
}

fun ByteArray.getInt32LE(offset: Int): Int {
    return ByteBuffer.wrap(this, offset, 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .int
}

/**
 * Temperature conversion utilities
 */

/**
 * Converts raw byte to temperature in Celsius (Motor/Solar devices use +40°C offset)
 * 0x00 = -40°C, 0xFF = +215°C
 */
fun UByte.toTemperatureWithOffset(): Int = this.toInt() - 40

/**
 * Converts temperature in Celsius to raw byte with offset
 */
fun Int.toTemperatureByteWithOffset(): UByte {
    require(this in -40..215) { "Temperature must be in range -40 to 215°C" }
    return (this + 40).toUByte()
}

/**
 * For BMS/Battery Monitor: signed int8_t, no offset
 * 0x7F = invalid sensor
 */
fun Byte.toTemperatureNoOffset(): Int? {
    return if (this == 0x7F.toByte()) null else this.toInt()
}

/**
 * Voltage conversion utilities
 */

/**
 * Converts raw uint16 to voltage in volts (0.1V units)
 */
fun UShort.toVoltage(): Float = this.toFloat() / 10.0f

/**
 * Converts voltage in volts to raw uint16 (0.1V units)
 */
fun Float.toVoltageUInt16(): UShort = (this * 10.0f).toInt().toUShort()

/**
 * Converts raw uint16 to voltage in volts (0.01V units)
 */
fun UShort.toVoltageHighRes(): Float = this.toFloat() / 100.0f

/**
 * Converts raw uint16 to voltage in millivolts
 */
fun UShort.toMillivolts(): Int = this.toInt()

/**
 * Current conversion utilities
 */

/**
 * Converts raw int16 to current in amperes (0.1A units)
 * Positive = discharge, Negative = charge
 */
fun Short.toCurrentSigned(): Float = this.toFloat() / 10.0f

/**
 * Converts current in amperes to raw int16 (0.1A units)
 */
fun Float.toCurrentInt16(): Short = (this * 10.0f).toInt().toShort()

/**
 * Converts raw uint16 to current in amperes (0.1A units)
 */
fun UShort.toCurrentUnsigned(): Float = this.toFloat() / 10.0f

/**
 * Power conversion utilities
 */

/**
 * Converts raw int16 to power in watts (10W units)
 */
fun Short.toPowerSigned(): Int = this.toInt() * 10

/**
 * Converts raw uint16 to power in watts (10W units)
 */
fun UShort.toPowerUnsigned(): Int = this.toInt() * 10

/**
 * Energy conversion utilities
 */

/**
 * Converts raw uint16 to energy in kilowatt-hours (0.01kWh units)
 */
fun UShort.toEnergyKWh(): Float = this.toFloat() / 100.0f

/**
 * Converts raw uint16 to energy in kilowatt-hours (0.1kWh units)
 */
fun UShort.toEnergyKWhLowRes(): Float = this.toFloat() / 10.0f

/**
 * GPS coordinate conversion utilities
 */

/**
 * Converts raw int32 to latitude in degrees (1e-7 degrees units)
 */
fun Int.toLatitude(): Double = this.toDouble() / 1e7

/**
 * Converts raw int24 to longitude in degrees (1e-6 degrees units)
 * Combined with byte offset from 0x648
 */
fun Int.toLongitude(offsetDegrees: Int = 0): Double {
    return (this.toDouble() / 1e6) + offsetDegrees.toDouble()
}

/**
 * Speed conversion utilities
 */

/**
 * Converts raw uint16 to speed in m/s (0.01 m/s units)
 */
fun UShort.toSpeed(): Float = this.toFloat() / 100.0f

/**
 * Converts speed in m/s to km/h
 */
fun Float.toKmh(): Float = this * 3.6f

/**
 * Converts speed in m/s to mph
 */
fun Float.toMph(): Float = this * 2.23694f

/**
 * Bitfield utilities
 */

/**
 * Checks if a specific bit is set
 */
fun UByte.isBitSet(bit: Int): Boolean {
    require(bit in 0..7) { "Bit index must be 0-7" }
    return (this.toInt() and (1 shl bit)) != 0
}

/**
 * Gets a range of bits as an integer value
 */
fun UByte.getBits(startBit: Int, numBits: Int): Int {
    require(startBit in 0..7) { "Start bit must be 0-7" }
    require(numBits in 1..8) { "Number of bits must be 1-8" }
    require(startBit + numBits <= 8) { "Bit range exceeds byte boundary" }

    val mask = (1 shl numBits) - 1
    return (this.toInt() shr startBit) and mask
}

/**
 * Sets a specific bit
 */
fun UByte.setBit(bit: Int, value: Boolean): UByte {
    require(bit in 0..7) { "Bit index must be 0-7" }
    return if (value) {
        (this.toInt() or (1 shl bit)).toUByte()
    } else {
        (this.toInt() and (1 shl bit).inv()).toUByte()
    }
}

/**
 * Capacity conversion utilities
 */

/**
 * Converts raw uint16 to capacity in amp-hours (0.1Ah units)
 */
fun UShort.toCapacityAh(): Float = this.toFloat() / 10.0f

/**
 * Time conversion utilities
 */

/**
 * Converts subsecond byte to milliseconds
 * Range: 0-255 represents 0-1000ms (ms ÷ 4)
 */
fun UByte.toSubsecondMs(): Int = this.toInt() * 4

/**
 * Converts milliseconds to subsecond byte
 */
fun Int.toSubsecondByte(): UByte {
    require(this in 0..1000) { "Milliseconds must be 0-1000" }
    return (this / 4).toUByte()
}
