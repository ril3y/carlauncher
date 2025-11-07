package io.battlewithbytes.carlauncher.can.protocol

/**
 * CRC8 calculator for CAN message validation
 * Polynomial: 0x07, Init: 0x00
 * Applied to bytes 0-6 only (byte 7 is checksum)
 */
object Crc8Calculator {
    private const val POLYNOMIAL: Int = 0x07

    /**
     * Calculate CRC8 checksum for data bytes
     * @param data Complete message data (8 bytes)
     * @return Calculated CRC8 value
     */
    fun calculate(data: ByteArray): Byte {
        require(data.size >= 7) { "Data must be at least 7 bytes for CRC calculation" }

        var crc = 0
        for (i in 0 until 7) {  // Only bytes 0-6
            crc = crc xor (data[i].toInt() and 0xFF)
            for (bit in 0 until 8) {
                crc = if ((crc and 0x80) != 0) {
                    (crc shl 1) xor POLYNOMIAL
                } else {
                    crc shl 1
                }
            }
        }
        return (crc and 0xFF).toByte()
    }

    /**
     * Validate CRC8 checksum
     * @param data Complete message data (8 bytes)
     * @return True if CRC in byte 7 matches calculated CRC
     */
    fun validate(data: ByteArray): Boolean {
        if (data.size < 8) return false
        val calculated = calculate(data)
        val provided = data[7]
        return calculated == provided
    }
}
