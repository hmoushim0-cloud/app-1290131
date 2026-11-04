package sa.factory.app1290131.hardware

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.roundToInt

data class LabPhotometerFrame(
    val sequenceNumber: Int,
    val wavelengthNm: Int,
    val transmittancePct: Double,
    val absorbance: Double,
    val solutionPh: Double,
    val temperatureCelsius: Double,
    val redoxPotentialMv: Short,
    val molarConcentrationM: Double,
    val isValidCrc: Boolean
)

data class BeerLambertCalibration(
    val standardAnalyteName: String,
    val molarAbsorptivityLPerMolCm: Double,
    val pathLengthCm: Double = 1.0
)

class LabColorimeterPhotometerDriver {

    companion object {
        const val MAGIC_HEADER: Short = 0xC845.toShort() // "CH"
        const val PROTOCOL_VERSION: Byte = 0x02
        const val FRAME_SIZE_BYTES: Int = 16
    }

    fun computeCrc16Ccitt(data: ByteArray, offset: Int, length: Int): Short {
        var crc = 0xFFFF
        for (i in offset until (offset + length)) {
            val b = data[i].toInt() and 0xFF
            crc = crc xor (b shl 8)
            for (bit in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return (crc and 0xFFFF).toShort()
    }

    fun serializeFrame(
        sequenceNumber: Int,
        wavelengthNm: Int,
        transmittancePct: Double,
        solutionPh: Double,
        temperatureCelsius: Double,
        redoxPotentialMv: Short
    ): ByteArray {
        val buffer = ByteBuffer.allocate(FRAME_SIZE_BYTES).order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(MAGIC_HEADER)
        buffer.put(PROTOCOL_VERSION)
        buffer.put((sequenceNumber and 0xFF).toByte())
        buffer.putShort(wavelengthNm.toShort())

        val tInt = (transmittancePct * 100.0).roundToInt().coerceIn(0, 10000)
        buffer.putShort(tInt.toShort())

        val phInt = (solutionPh * 100.0).roundToInt().coerceIn(0, 1400)
        buffer.putShort(phInt.toShort())

        val tempInt = (temperatureCelsius * 10.0).roundToInt().toShort()
        buffer.putShort(tempInt)

        buffer.putShort(redoxPotentialMv)

        val crc = computeCrc16Ccitt(buffer.array(), 0, FRAME_SIZE_BYTES - 2)
        buffer.putShort(crc)

        return buffer.array()
    }

    fun parseFrame(
        packet: ByteArray,
        calibration: BeerLambertCalibration = BeerLambertCalibration("Copper(II) Sulfate", 2.81, 1.0)
    ): LabPhotometerFrame {
        if (packet.size < FRAME_SIZE_BYTES) {
            throw IllegalArgumentException("Packet too short for lab photometer frame: ${packet.size} bytes")
        }

        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        val magic = buffer.short
        if (magic != MAGIC_HEADER) {
            throw IllegalArgumentException("Invalid photometer magic header: 0x${Integer.toHexString(magic.toInt() and 0xFFFF)}")
        }

        val version = buffer.get()
        if (version != PROTOCOL_VERSION) {
            throw IllegalArgumentException("Unsupported photometer driver version: $version")
        }

        val seq = buffer.get().toInt() and 0xFF
        val wavelength = buffer.short.toInt() and 0xFFFF
        val rawT = buffer.short.toInt() and 0xFFFF
        val rawPh = buffer.short.toInt() and 0xFFFF
        val rawTemp = buffer.short.toInt()
        val orp = buffer.short

        val packetCrc = buffer.short
        val computedCrc = computeCrc16Ccitt(packet, 0, FRAME_SIZE_BYTES - 2)
        val isValidCrc = packetCrc == computedCrc

        val transmittance = rawT / 100.0
        val ph = rawPh / 100.0
        val temp = rawTemp / 10.0

        // Absorbance A = -log10(T / 100)
        val absorbance = if (transmittance > 0.0) {
            -log10(transmittance / 100.0)
        } else {
            3.0 // Saturation cap
        }

        // Beer-Lambert Law: c = A / (epsilon * b)
        val molarConc = if (calibration.molarAbsorptivityLPerMolCm > 0.0) {
            absorbance / (calibration.molarAbsorptivityLPerMolCm * calibration.pathLengthCm)
        } else {
            0.0
        }

        return LabPhotometerFrame(
            sequenceNumber = seq,
            wavelengthNm = wavelength,
            transmittancePct = (transmittance * 100.0).roundToInt() / 100.0,
            absorbance = (absorbance * 1000.0).roundToInt() / 1000.0,
            solutionPh = ph,
            temperatureCelsius = temp,
            redoxPotentialMv = orp,
            molarConcentrationM = (molarConc * 10000.0).roundToInt() / 10000.0,
            isValidCrc = isValidCrc
        )
    }
}
