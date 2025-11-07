package io.battlewithbytes.carlauncher.can.messages

import io.battlewithbytes.carlauncher.can.*

/**
 * Motor Control Messages (0x610-0x613)
 * Motor 1: 0x610-0x611, Motor 2: 0x612-0x613
 */

/**
 * 0x610 / 0x612 - Motor Basic Telemetry
 * Transmission: 10Hz
 */
data class MotorTelemetryMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,
    override val canId: Int,

    // Bytes 0-1: RPM (uint16 little-endian)
    val rpm: UShort,

    // Bytes 2-3: Current (uint16 × 0.1A)
    val currentAmps: Float,

    // Byte 4: Motor temperature (with +40°C offset)
    val motorTemperatureCelsius: Int,

    // Byte 5: Controller temperature (with +40°C offset)
    val controllerTemperatureCelsius: Int,

    // Byte 6: Throttle (0-100%)
    val throttlePercent: UByte

) : CanMessage {

    companion object {
        const val MOTOR_1_CAN_ID = 0x610
        const val MOTOR_2_CAN_ID = 0x612
        const val UPDATE_RATE_MS = 100L // 10Hz
        const val MAX_AGE_MS = 300L // Stale after 300ms

        /**
         * Parses a CAN frame into a MotorTelemetryMessage
         */
        fun parse(canId: Int, data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<MotorTelemetryMessage> {
            if (canId != MOTOR_1_CAN_ID && canId != MOTOR_2_CAN_ID) {
                return ParseResult.InvalidData(canId, "Invalid motor telemetry CAN ID: 0x${canId.toString(16)}")
            }

            if (data.size != 8) {
                return ParseResult.InvalidData(canId, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(canId, data)
            }

            val rpm = data.getUInt16LE(0)
            val currentRaw = data.getUInt16LE(2)
            val motorTemp = data.getUInt8(4)
            val controllerTemp = data.getUInt8(5)
            val throttle = data.getUInt8(6)

            val message = MotorTelemetryMessage(
                timestamp = timestamp,
                isValid = true,
                canId = canId,
                rpm = rpm,
                currentAmps = currentRaw.toCurrentUnsigned(),
                motorTemperatureCelsius = motorTemp.toTemperatureWithOffset(),
                controllerTemperatureCelsius = controllerTemp.toTemperatureWithOffset(),
                throttlePercent = throttle
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets motor number (1 or 2)
     */
    fun getMotorNumber(): Int = if (canId == MOTOR_1_CAN_ID) 1 else 2

    /**
     * Checks if motor is running
     */
    fun isRunning(): Boolean = rpm > 0u && throttlePercent > 0u

    /**
     * Checks if temperatures are within safe operating range
     */
    fun isTemperatureOk(maxMotorTemp: Int = 100, maxControllerTemp: Int = 70): Boolean {
        return motorTemperatureCelsius <= maxMotorTemp && controllerTemperatureCelsius <= maxControllerTemp
    }

    /**
     * Checks if motor is overheating
     */
    fun isOverheating(motorLimit: Int = 100, controllerLimit: Int = 70): Boolean {
        return motorTemperatureCelsius > motorLimit || controllerTemperatureCelsius > controllerLimit
    }

    /**
     * Calculates estimated power in watts
     * Assumes nominal 48V system (actual voltage should come from BMS)
     */
    fun estimatePowerWatts(voltageVolts: Float = 48.0f): Float {
        return currentAmps * voltageVolts
    }
}

/**
 * Status flags for motor status message (Byte 4)
 */
data class MotorStatusFlags(
    val regenActive: Boolean,           // Bit 0
    val throttleActive: Boolean,        // Bit 1
    val currentLimiting: Boolean,       // Bit 2
    val voltageLimiting: Boolean,       // Bit 3
    val hallSensorsOk: Boolean,         // Bit 4
    val directionForward: Boolean,      // Bit 5: 1=Forward, 0=Reverse
    val motorRunning: Boolean           // Bit 6
    // Bit 7: Reserved
) {
    companion object {
        fun fromByte(byte: UByte): MotorStatusFlags {
            return MotorStatusFlags(
                regenActive = byte.isBitSet(0),
                throttleActive = byte.isBitSet(1),
                currentLimiting = byte.isBitSet(2),
                voltageLimiting = byte.isBitSet(3),
                hallSensorsOk = byte.isBitSet(4),
                directionForward = byte.isBitSet(5),
                motorRunning = byte.isBitSet(6)
            )
        }
    }
}

/**
 * Fault flags for motor status message (Byte 5)
 */
data class MotorFaultFlags(
    val overvoltage: Boolean,           // Bit 0
    val undervoltage: Boolean,          // Bit 1
    val overcurrent: Boolean,           // Bit 2
    val controllerOvertemp: Boolean,    // Bit 3
    val motorOvertemp: Boolean,         // Bit 4
    val throttleFault: Boolean,         // Bit 5
    val hallSensorFault: Boolean,       // Bit 6
    val generalFault: Boolean           // Bit 7
) {
    companion object {
        fun fromByte(byte: UByte): MotorFaultFlags {
            return MotorFaultFlags(
                overvoltage = byte.isBitSet(0),
                undervoltage = byte.isBitSet(1),
                overcurrent = byte.isBitSet(2),
                controllerOvertemp = byte.isBitSet(3),
                motorOvertemp = byte.isBitSet(4),
                throttleFault = byte.isBitSet(5),
                hallSensorFault = byte.isBitSet(6),
                generalFault = byte.isBitSet(7)
            )
        }
    }

    /**
     * Checks if any fault is active
     */
    fun hasFault(): Boolean {
        return overvoltage || undervoltage || overcurrent || controllerOvertemp ||
               motorOvertemp || throttleFault || hallSensorFault || generalFault
    }

    /**
     * Checks if any critical fault is active
     */
    fun hasCriticalFault(): Boolean {
        return overvoltage || undervoltage || overcurrent || controllerOvertemp || motorOvertemp
    }

    /**
     * Gets list of active faults
     */
    fun getActiveFaults(): List<String> {
        val faults = mutableListOf<String>()
        if (overvoltage) faults.add("Overvoltage")
        if (undervoltage) faults.add("Undervoltage")
        if (overcurrent) faults.add("Overcurrent")
        if (controllerOvertemp) faults.add("Controller Overtemp")
        if (motorOvertemp) faults.add("Motor Overtemp")
        if (throttleFault) faults.add("Throttle Fault")
        if (hallSensorFault) faults.add("Hall Sensor Fault")
        if (generalFault) faults.add("General Fault")
        return faults
    }
}

/**
 * 0x611 / 0x613 - Motor Status & Faults
 * Transmission: 1Hz
 */
data class MotorStatusMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,
    override val canId: Int,

    // Bytes 0-1: Battery voltage (uint16 × 0.1V)
    val batteryVoltageVolts: Float,

    // Byte 2: Controller temperature (with +40°C offset)
    val controllerTemperatureCelsius: Int,

    // Byte 3: PWM duty cycle (0-100%)
    val pwmDutyCycle: UByte,

    // Byte 4: Status flags
    val statusFlags: MotorStatusFlags,

    // Byte 5: Fault flags
    val faultFlags: MotorFaultFlags

    // Byte 6: Reserved (0x00)

) : CanMessage {

    companion object {
        const val MOTOR_1_CAN_ID = 0x611
        const val MOTOR_2_CAN_ID = 0x613
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L // Stale after 3 seconds

        /**
         * Parses a CAN frame into a MotorStatusMessage
         */
        fun parse(canId: Int, data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<MotorStatusMessage> {
            if (canId != MOTOR_1_CAN_ID && canId != MOTOR_2_CAN_ID) {
                return ParseResult.InvalidData(canId, "Invalid motor status CAN ID: 0x${canId.toString(16)}")
            }

            if (data.size != 8) {
                return ParseResult.InvalidData(canId, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(canId, data)
            }

            val voltageRaw = data.getUInt16LE(0)
            val controllerTemp = data.getUInt8(2)
            val pwmDuty = data.getUInt8(3)
            val statusByte = data.getUInt8(4)
            val faultByte = data.getUInt8(5)

            val message = MotorStatusMessage(
                timestamp = timestamp,
                isValid = true,
                canId = canId,
                batteryVoltageVolts = voltageRaw.toVoltage(),
                controllerTemperatureCelsius = controllerTemp.toTemperatureWithOffset(),
                pwmDutyCycle = pwmDuty,
                statusFlags = MotorStatusFlags.fromByte(statusByte),
                faultFlags = MotorFaultFlags.fromByte(faultByte)
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets motor number (1 or 2)
     */
    fun getMotorNumber(): Int = if (canId == MOTOR_1_CAN_ID) 1 else 2

    /**
     * Checks if motor is OK (no faults, normal operation)
     */
    fun isOk(): Boolean = !faultFlags.hasFault()

    /**
     * Checks if motor has any faults
     */
    fun hasFault(): Boolean = faultFlags.hasFault()

    /**
     * Checks if motor has critical faults
     */
    fun hasCriticalFault(): Boolean = faultFlags.hasCriticalFault()

    /**
     * Checks if motor is running
     */
    fun isRunning(): Boolean = statusFlags.motorRunning

    /**
     * Checks if regenerative braking is active
     */
    fun isRegenerating(): Boolean = statusFlags.regenActive

    /**
     * Gets the current direction
     */
    fun getDirection(): String = if (statusFlags.directionForward) "Forward" else "Reverse"

    /**
     * Checks if voltage is within acceptable range
     */
    fun isVoltageOk(minVoltage: Float = 42.0f, maxVoltage: Float = 58.0f): Boolean {
        return batteryVoltageVolts in minVoltage..maxVoltage
    }
}

/**
 * Combined motor state for a single motor
 */
data class MotorState(
    val telemetry: MotorTelemetryMessage? = null,
    val status: MotorStatusMessage? = null
) {
    /**
     * Checks if we have recent data for both messages
     */
    fun isDataFresh(maxAgeMs: Long = MotorTelemetryMessage.MAX_AGE_MS): Boolean {
        return telemetry?.isStale(maxAgeMs) == false &&
               status?.isStale(MotorStatusMessage.MAX_AGE_MS) == false
    }

    /**
     * Checks if motor is healthy and operational
     */
    fun isHealthy(): Boolean {
        return telemetry?.isTemperatureOk() == true &&
               status?.isOk() == true &&
               isDataFresh()
    }

    /**
     * Gets overall motor health status
     */
    fun getHealth(): MotorHealth {
        return when {
            status?.hasCriticalFault() == true -> MotorHealth.CRITICAL
            status?.hasFault() == true -> MotorHealth.FAULT
            telemetry?.isOverheating() == true -> MotorHealth.WARNING
            !isDataFresh() -> MotorHealth.STALE
            isHealthy() -> MotorHealth.OK
            else -> MotorHealth.UNKNOWN
        }
    }

    /**
     * Updates with new telemetry message
     */
    fun updateTelemetry(msg: MotorTelemetryMessage): MotorState {
        return copy(telemetry = msg)
    }

    /**
     * Updates with new status message
     */
    fun updateStatus(msg: MotorStatusMessage): MotorState {
        return copy(status = msg)
    }
}

/**
 * Motor health status
 */
enum class MotorHealth {
    OK,         // All normal
    WARNING,    // Warning condition (high temp, etc)
    FAULT,      // Non-critical fault
    CRITICAL,   // Critical fault
    STALE,      // Data too old
    UNKNOWN     // Insufficient data
}

/**
 * Dual motor state tracker
 */
data class DualMotorState(
    val motor1: MotorState = MotorState(),
    val motor2: MotorState = MotorState()
) {
    /**
     * Checks if both motors are healthy
     */
    fun allHealthy(): Boolean {
        return motor1.isHealthy() && motor2.isHealthy()
    }

    /**
     * Gets combined power output in watts
     */
    fun getTotalPowerWatts(voltageVolts: Float = 48.0f): Float {
        val power1 = motor1.telemetry?.estimatePowerWatts(voltageVolts) ?: 0.0f
        val power2 = motor2.telemetry?.estimatePowerWatts(voltageVolts) ?: 0.0f
        return power1 + power2
    }

    /**
     * Gets combined current draw
     */
    fun getTotalCurrentAmps(): Float {
        val current1 = motor1.telemetry?.currentAmps ?: 0.0f
        val current2 = motor2.telemetry?.currentAmps ?: 0.0f
        return current1 + current2
    }

    /**
     * Updates with a motor message
     */
    fun updateWith(msg: CanMessage): DualMotorState {
        return when (msg) {
            is MotorTelemetryMessage -> {
                if (msg.getMotorNumber() == 1) {
                    copy(motor1 = motor1.updateTelemetry(msg))
                } else {
                    copy(motor2 = motor2.updateTelemetry(msg))
                }
            }
            is MotorStatusMessage -> {
                if (msg.getMotorNumber() == 1) {
                    copy(motor1 = motor1.updateStatus(msg))
                } else {
                    copy(motor2 = motor2.updateStatus(msg))
                }
            }
            else -> this
        }
    }
}
