package io.battlewithbytes.carlauncher.can.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Sealed hierarchy for type-safe CAN message parsing
 * Golf Cart Protocol v1.6 - 46 message types (0x500-0x655)
 */
sealed class CanMessage {
    abstract val id: Int
    abstract val timestamp: Long
    abstract val crcValid: Boolean

    /**
     * BMS Pack Status - 0x620 (10Hz CRITICAL)
     * Battery management system pack-level telemetry
     */
    data class BmsPackStatus(
        override val id: Int = 0x620,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val packVoltageV: Float,          // uint16 * 0.1V
        val packCurrentA: Float,          // int16 * 0.1A (positive=discharge)
        val soc: Int,                     // 0-100%
        val socLow: Int,                  // Lower byte for precision
        val flags: Byte,
        val sequence: Int
    ) : CanMessage() {
        val isCharging: Boolean get() = packCurrentA < 0
        val powerW: Float get() = packVoltageV * packCurrentA
    }

    /**
     * Battery Monitor Current/Power - 0x650 (10Hz CRITICAL AUTHORITATIVE)
     * Authoritative current measurement from shunt
     */
    data class PackCurrentPower(
        override val id: Int = 0x650,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val packCurrentA: Float,          // int16 * 0.1A (AUTHORITATIVE)
        val instantPowerW: Float,         // int16 * 10W
        val packVoltageV: Float,          // uint16 * 0.1V
        val loadPercent: Int              // 0-100%
    ) : CanMessage()

    /**
     * SOC & Capacity - 0x651 (1Hz CRITICAL AUTHORITATIVE)
     * Authoritative state of charge from coulomb counting
     */
    data class SocCapacity(
        override val id: Int = 0x651,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val soc: Int,                     // 0-100% AUTHORITATIVE SOC
        val remainingCapacityAh: Float,   // uint16 * 0.1Ah
        val fullCapacityAh: Float,        // uint16 * 0.1Ah
        val socConfidence: Int,           // 0-100%
        val syncFlags: Byte
    ) : CanMessage() {
        val bmsFullChargeSeen: Boolean get() = (syncFlags.toInt() and 0x20) != 0
        val coulombSynced: Boolean get() = (syncFlags.toInt() and 0x40) != 0
    }

    /**
     * Motor Telemetry - 0x610, 0x612 (10Hz)
     * Real-time motor performance data
     */
    data class MotorBasic(
        override val id: Int,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val rpm: Int,                     // uint16
        val currentA: Float,              // uint16 * 0.1A
        val motorTempC: Int,              // byte + 40°C
        val controllerTempC: Int,         // byte + 40°C
        val throttlePercent: Int          // 0-100%
    ) : CanMessage() {
        val motorNumber: Int get() = if (id == 0x610) 1 else 2
    }

    /**
     * Motor Status & Faults - 0x611, 0x613 (1Hz)
     */
    data class MotorStatus(
        override val id: Int,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val batteryVoltageV: Float,       // uint16 * 0.1V
        val controllerTempC: Int,         // byte + 40°C
        val pwmDuty: Int,                 // 0-100%
        val statusFlags: Byte,
        val faultFlags: Byte
    ) : CanMessage() {
        val motorNumber: Int get() = if (id == 0x611) 1 else 2
        val hasFault: Boolean get() = faultFlags.toInt() != 0
        val running: Boolean get() = (statusFlags.toInt() and 0x40) != 0
    }

    /**
     * Solar Power Status - 0x630 (10Hz CRITICAL)
     */
    data class SolarPowerStatus(
        override val id: Int = 0x630,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val batteryVoltageV: Float,       // uint16 * 0.1V
        val chargingCurrentA: Float,      // uint16 * 0.1A
        val solarVoltageV: Float,         // uint8 * 0.5V
        val chargingPowerW: Int           // uint8 * 10W
    ) : CanMessage()

    /**
     * GPS Position - 0x645 (1Hz when locked)
     */
    data class GpsPosition(
        override val id: Int = 0x645,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val latitude: Double,             // int32 * 1e-7 degrees
        val longitude: Double             // int24 * 1e-6 degrees
    ) : CanMessage()

    /**
     * GPS Velocity - 0x646 (1Hz when locked)
     */
    data class GpsVelocity(
        override val id: Int = 0x646,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val groundSpeedMps: Float,        // uint16 * 0.01 m/s
        val courseDeg: Float,             // uint16 * 0.01 degrees
        val verticalSpeedMps: Float       // int8 * 0.1 m/s
    ) : CanMessage() {
        val speedKmh: Float get() = groundSpeedMps * 3.6f
    }

    /**
     * Heartbeat - 0x600-0x607 (1Hz)
     */
    data class Heartbeat(
        override val id: Int,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val statusCode: Int,              // 0x01=OK, 0x00=ERROR
        val errorCount: Int,
        val currentError: Byte,
        val protocolVersion: Int,
        val temperatureC: Int,            // byte + 40°C
        val uptime: Int
    ) : CanMessage() {
        val deviceName: String
            get() = when (id) {
                0x600 -> "Wiring Harness"
                0x601 -> "Motor Controller 1"
                0x602 -> "Motor Controller 2"
                0x603 -> "Display"
                0x604 -> "BMS"
                0x605 -> "Solar Controller"
                0x606 -> "GPS Module"
                0x607 -> "Battery Monitor"
                else -> "Unknown"
            }
        val isHealthy: Boolean get() = statusCode == 0x01
    }

    /**
     * Switch States - 0x500 (Event + 1Hz keepalive)
     */
    data class SwitchState(
        override val id: Int = 0x500,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val brake: Boolean,
        val eco: Boolean,
        val reverse: Boolean,
        val foot: Boolean,
        val forward: Boolean,
        val sequence: Int
    ) : CanMessage()

    /**
     * BMS Critical Alarms - 0x624
     */
    data class BmsCriticalAlarms(
        override val id: Int = 0x624,
        override val timestamp: Long,
        override val crcValid: Boolean,
        val voltageAlarms: Byte,
        val currentAlarms: Byte,
        val tempAlarms: Byte,
        val systemAlarms: Byte,
        val mosfetFailures: Byte
    ) : CanMessage() {
        val hasAnyAlarm: Boolean
            get() = voltageAlarms.toInt() or currentAlarms.toInt() or
                    tempAlarms.toInt() or systemAlarms.toInt() or
                    mosfetFailures.toInt() != 0
    }

    /**
     * Unknown/unparseable message
     */
    data class Unknown(
        override val id: Int,
        override val timestamp: Long,
        override val crcValid: Boolean = false,
        val rawData: ByteArray
    ) : CanMessage()

    companion object {
        /**
         * Parse raw CAN frame data into typed message
         */
        fun parse(canId: Int, data: ByteArray, timestamp: Long = System.currentTimeMillis()): CanMessage {
            if (data.size != 8) {
                return Unknown(canId, timestamp, false, data)
            }

            val crcValid = Crc8Calculator.validate(data)
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            return try {
                when (canId) {
                    0x500 -> parseSwitchState(data, timestamp, crcValid)
                    in 0x600..0x607 -> parseHeartbeat(canId, data, timestamp, crcValid)
                    0x610, 0x612 -> parseMotorBasic(canId, buffer, timestamp, crcValid)
                    0x611, 0x613 -> parseMotorStatus(canId, buffer, timestamp, crcValid)
                    0x620 -> parseBmsPackStatus(buffer, timestamp, crcValid)
                    0x624 -> parseBmsCriticalAlarms(data, timestamp, crcValid)
                    0x630 -> parseSolarPowerStatus(buffer, timestamp, crcValid)
                    0x645 -> parseGpsPosition(buffer, data, timestamp, crcValid)
                    0x646 -> parseGpsVelocity(buffer, data, timestamp, crcValid)
                    0x650 -> parsePackCurrentPower(buffer, timestamp, crcValid)
                    0x651 -> parseSocCapacity(buffer, data, timestamp, crcValid)
                    else -> Unknown(canId, timestamp, crcValid, data)
                }
            } catch (e: Exception) {
                Unknown(canId, timestamp, false, data)
            }
        }

        private fun parseBmsPackStatus(buffer: ByteBuffer, timestamp: Long, crcValid: Boolean): BmsPackStatus {
            return BmsPackStatus(
                timestamp = timestamp,
                crcValid = crcValid,
                packVoltageV = buffer.getShort(0).toUShort().toFloat() * 0.1f,
                packCurrentA = buffer.getShort(2).toFloat() * 0.1f,
                soc = buffer.get(4).toUByte().toInt(),
                socLow = buffer.get(5).toUByte().toInt(),
                flags = buffer.get(6),
                sequence = buffer.get(7).toUByte().toInt()
            )
        }

        private fun parsePackCurrentPower(buffer: ByteBuffer, timestamp: Long, crcValid: Boolean): PackCurrentPower {
            return PackCurrentPower(
                timestamp = timestamp,
                crcValid = crcValid,
                packCurrentA = buffer.getShort(0).toFloat() * 0.1f,
                instantPowerW = buffer.getShort(2).toFloat() * 10f,
                packVoltageV = buffer.getShort(4).toUShort().toFloat() * 0.1f,
                loadPercent = buffer.get(6).toUByte().toInt()
            )
        }

        private fun parseSocCapacity(buffer: ByteBuffer, data: ByteArray, timestamp: Long, crcValid: Boolean): SocCapacity {
            return SocCapacity(
                timestamp = timestamp,
                crcValid = crcValid,
                soc = data[0].toUByte().toInt(),
                remainingCapacityAh = buffer.getShort(1).toUShort().toFloat() * 0.1f,
                fullCapacityAh = buffer.getShort(3).toUShort().toFloat() * 0.1f,
                socConfidence = data[5].toUByte().toInt(),
                syncFlags = data[6]
            )
        }

        private fun parseMotorBasic(canId: Int, buffer: ByteBuffer, timestamp: Long, crcValid: Boolean): MotorBasic {
            return MotorBasic(
                id = canId,
                timestamp = timestamp,
                crcValid = crcValid,
                rpm = buffer.getShort(0).toUShort().toInt(),
                currentA = buffer.getShort(2).toUShort().toFloat() * 0.1f,
                motorTempC = buffer.get(4).toUByte().toInt() - 40,
                controllerTempC = buffer.get(5).toUByte().toInt() - 40,
                throttlePercent = buffer.get(6).toUByte().toInt()
            )
        }

        private fun parseMotorStatus(canId: Int, buffer: ByteBuffer, timestamp: Long, crcValid: Boolean): MotorStatus {
            return MotorStatus(
                id = canId,
                timestamp = timestamp,
                crcValid = crcValid,
                batteryVoltageV = buffer.getShort(0).toUShort().toFloat() * 0.1f,
                controllerTempC = buffer.get(2).toUByte().toInt() - 40,
                pwmDuty = buffer.get(3).toUByte().toInt(),
                statusFlags = buffer.get(4),
                faultFlags = buffer.get(5)
            )
        }

        private fun parseSolarPowerStatus(buffer: ByteBuffer, timestamp: Long, crcValid: Boolean): SolarPowerStatus {
            return SolarPowerStatus(
                timestamp = timestamp,
                crcValid = crcValid,
                batteryVoltageV = buffer.getShort(0).toUShort().toFloat() * 0.1f,
                chargingCurrentA = buffer.getShort(2).toUShort().toFloat() * 0.1f,
                solarVoltageV = buffer.get(4).toUByte().toFloat() * 0.5f,
                chargingPowerW = buffer.get(5).toUByte().toInt() * 10
            )
        }

        private fun parseGpsPosition(buffer: ByteBuffer, data: ByteArray, timestamp: Long, crcValid: Boolean): GpsPosition {
            val latInt = buffer.getInt(0)
            val longitude24 = ((data[4].toInt() and 0xFF) or
                    ((data[5].toInt() and 0xFF) shl 8) or
                    ((data[6].toInt() and 0xFF) shl 16))
            val lonInt = if (longitude24 and 0x800000 != 0) {
                longitude24 or 0xFF000000.toInt()
            } else {
                longitude24
            }

            return GpsPosition(
                timestamp = timestamp,
                crcValid = crcValid,
                latitude = latInt.toDouble() * 1e-7,
                longitude = lonInt.toDouble() * 1e-6
            )
        }

        private fun parseGpsVelocity(buffer: ByteBuffer, data: ByteArray, timestamp: Long, crcValid: Boolean): GpsVelocity {
            return GpsVelocity(
                timestamp = timestamp,
                crcValid = crcValid,
                groundSpeedMps = buffer.getShort(0).toUShort().toFloat() * 0.01f,
                courseDeg = buffer.getShort(2).toUShort().toFloat() * 0.01f,
                verticalSpeedMps = data[4].toFloat() * 0.1f
            )
        }

        private fun parseHeartbeat(canId: Int, data: ByteArray, timestamp: Long, crcValid: Boolean): Heartbeat {
            return Heartbeat(
                id = canId,
                timestamp = timestamp,
                crcValid = crcValid,
                statusCode = data[0].toUByte().toInt(),
                errorCount = data[1].toUByte().toInt(),
                currentError = data[2],
                protocolVersion = data[3].toUByte().toInt(),
                temperatureC = data[4].toUByte().toInt() - 40,
                uptime = data[6].toUByte().toInt()
            )
        }

        private fun parseSwitchState(data: ByteArray, timestamp: Long, crcValid: Boolean): SwitchState {
            val bitmap = data[0].toUByte().toInt()
            return SwitchState(
                timestamp = timestamp,
                crcValid = crcValid,
                brake = (bitmap and 0x01) != 0,
                eco = (bitmap and 0x02) != 0,
                reverse = (bitmap and 0x04) != 0,
                foot = (bitmap and 0x08) != 0,
                forward = (bitmap and 0x10) != 0,
                sequence = data[1].toUByte().toInt()
            )
        }

        private fun parseBmsCriticalAlarms(data: ByteArray, timestamp: Long, crcValid: Boolean): BmsCriticalAlarms {
            return BmsCriticalAlarms(
                timestamp = timestamp,
                crcValid = crcValid,
                voltageAlarms = data[0],
                currentAlarms = data[1],
                tempAlarms = data[2],
                systemAlarms = data[3],
                mosfetFailures = data[4]
            )
        }

        // Extension functions for unsigned byte handling
        private fun Byte.toUByte(): UByte = this.toUByte()
        private fun Short.toUShort(): UShort = this.toUShort()
    }
}
