package io.battlewithbytes.carlauncher.model

/**
 * Represents a CAN bus message
 */
data class CanMessage(
    val id: Int,
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CanMessage

        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Base interface for all vehicle data types
 */
interface VehicleData {
    val timestamp: Long
}

/**
 * Battery information from CAN bus
 */
data class BatteryData(
    val voltageV: Float,
    val currentA: Float,
    val stateOfChargePercent: Float,
    val temperatureC: Float,
    override val timestamp: Long = System.currentTimeMillis()
) : VehicleData

/**
 * Motor information from CAN bus
 */
data class MotorData(
    val rpm: Int,
    val temperatureC: Float,
    val currentA: Float,
    val powerKw: Float,
    val faultCode: Int = 0,
    override val timestamp: Long = System.currentTimeMillis()
) : VehicleData

/**
 * Vehicle speed and odometer
 */
data class VehicleStatus(
    val speedKmh: Float,
    val odometerKm: Float,
    val tripMeterKm: Float,
    override val timestamp: Long = System.currentTimeMillis()
) : VehicleData
