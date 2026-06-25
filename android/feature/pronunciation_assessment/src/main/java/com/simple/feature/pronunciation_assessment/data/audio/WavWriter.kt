package com.simple.feature.pronunciation_assessment.data.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Ghi PCM float32 ra WAV 16-bit mono.
 */
internal object WavWriter {

    /**
     * Lưu [samples] thành WAV ở [outFile] với [sampleRate].
     *
     * @return absolute path của file đã ghi
     */
    fun write(outFile: File, samples: FloatArray, sampleRate: Int): String {
        val dataSize = samples.size * 2 // 16-bit = 2 bytes/sample
        val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        // ── RIFF header ─────────────────────────
        buf.put("RIFF".toByteArray())
        buf.putInt(36 + dataSize)
        buf.put("WAVE".toByteArray())
        // ── fmt subchunk ────────────────────────
        buf.put("fmt ".toByteArray())
        buf.putInt(16)                      // Subchunk1Size (PCM)
        buf.putShort(1)                     // AudioFormat = PCM
        buf.putShort(1)                     // NumChannels  = mono
        buf.putInt(sampleRate)
        buf.putInt(sampleRate * 2)          // ByteRate
        buf.putShort(2)                     // BlockAlign
        buf.putShort(16)                    // BitsPerSample
        // ── data subchunk ───────────────────────
        buf.put("data".toByteArray())
        buf.putInt(dataSize)
        for (f in samples) {
            buf.putShort((f * 32767f).toInt().coerceIn(-32768, 32767).toShort())
        }

        FileOutputStream(outFile).use { it.write(buf.array()) }
        return outFile.absolutePath
    }
}
