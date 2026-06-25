/**
 * Wav2Vec2Phoneme.kt
 *
 * Chạy model wav2vec2-lv-60-espeak-cv-ft qua OnnxRuntime Android
 * để chuyển audio float32 → chuỗi IPA phoneme.
 *
 * Model: facebook/wav2vec2-lv-60-espeak-cv-ft
 *   - Input:  float32[1, num_samples]  (16kHz, mono, normalized zero-mean unit-var)
 *   - Output: float32[1, time_steps, 392]  (logits trên vocab IPA)
 *   - Decode: CTC greedy → list IPA phonemes
 *
 * Cách tải model:
 *   1. Chạy tools/export_wav2vec2_onnx.py — script này export ONNX + dump
 *      vocab.json thật của tokenizer (392 token).
 *   2. Copy 2 file vào assets/:
 *         wav2vec2_phoneme.onnx
 *         wav2vec2_phoneme_vocab.json
 *
 * QUAN TRỌNG — vì sao phải load vocab từ JSON?
 *   Tokenizer của model này (Wav2Vec2PhonemeCTCTokenizer) có 392 token với
 *   thứ tự CỤ THỂ. Hardcode tay → sai 1 index = decode ra IPA hoàn toàn
 *   khác, scorer thấy substitution liên tục, điểm chìm về 10/100. Phải load
 *   từ chính vocab.json của tokenizer mới đúng.
 *
 *   Model này KHÔNG có word delimiter "|" (Wav2Vec2PhonemeCTCTokenizer
 *   xuất phoneme phẳng, không tách từ). Mọi logic word boundary ở phía
 *   acoustic phải bỏ — việc tách phoneme về từ là trách nhiệm của
 *   PronunciationScorer dựa trên alignment.
 *
 * build.gradle:
 *   implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.26.0'
 *
 * AndroidManifest.xml:
 *   <uses-permission android:name="android.permission.RECORD_AUDIO"/>
 */

package com.simple.feature.pronunciation_assessment.use_case

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.simple.phonetics.BRANCH
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.FloatBuffer
import kotlin.use

// ─────────────────────────────────────────────
// IPA Vocabulary — load từ assets/wav2vec2_phoneme_vocab.json
// ─────────────────────────────────────────────

/**
 * Vocabulary của tokenizer thật — load lúc init, không hardcode.
 *
 * Format file vocab.json (do tools/export_wav2vec2_onnx.py sinh):
 *   {
 *     "id_to_token": ["<pad>","<s>","</s>","<unk>","n","s","t",...,"a4"],
 *     "pad_id":      0,
 *     "special_ids": [0, 1, 2, 3],
 *     "word_delimiter_id": null
 *   }
 */
class Wav2Vec2Vocab(
    val idToToken: Array<String>,
    val padId:     Int,
    val specialIds: IntArray,
    val wordDelimiterId: Int   // -1 nếu model không có
) {
    val size: Int get() = idToToken.size

    private val specialSet: Set<Int> = specialIds.toHashSet()

    /** True nếu id là special token (<pad>, <s>, </s>, <unk>) hoặc word delimiter. */
    fun isSpecial(id: Int): Boolean =
        id in specialSet || (wordDelimiterId >= 0 && id == wordDelimiterId)

    fun decode(id: Int): String? = idToToken.getOrNull(id)

    companion object {
        const val ASSET_FILE_NAME = "wav2vec2_phoneme_vocab.json"

        fun loadFromAssets(context: Context, fileName: String = ASSET_FILE_NAME): Wav2Vec2Vocab {
            val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val root = JSONObject(json)

            val arr = root.getJSONArray("id_to_token")
            val tokens = Array(arr.length()) { arr.getString(it) }

            val padId = root.optInt("pad_id", 0)

            val specialArr = root.optJSONArray("special_ids")
            val specials = IntArray(specialArr?.length() ?: 0) { specialArr!!.getInt(it) }

            val wdt = if (root.isNull("word_delimiter_id")) -1 else root.optInt("word_delimiter_id", -1)

            return Wav2Vec2Vocab(
                idToToken         = tokens,
                padId             = padId,
                specialIds        = specials,
                wordDelimiterId   = wdt
            )
        }
    }
}

// ─────────────────────────────────────────────
// CTC Decoder
// ─────────────────────────────────────────────

/**
 * CTCDecoder — chuyển logits → IPA phoneme list.
 *
 * CTC Greedy Decode:
 *   1. Với mỗi time step, lấy argmax → token có xác suất cao nhất
 *   2. Collapse consecutive duplicates: [k,k,k,æ,æ,t] → [k,æ,t]
 *   3. Bỏ <pad> (blank) và các special token khác
 *
 * KHÔNG còn logic word boundary — model wav2vec2-lv-60-espeak-cv-ft
 * không có token "|", xuất phoneme phẳng. Tách từ làm ở tầng scorer
 * dựa trên alignment với reference IPA.
 */
object CTCDecoder {

    /**
     * @param logits Float32 array shape [time_steps × vocab_size]
     * @param vocabSize kích thước vocab (392 cho model này)
     * @param vocab    bảng decode id → IPA token
     */
    fun greedyDecode(
        logits: FloatArray,
        vocabSize: Int,
        vocab: Wav2Vec2Vocab
    ): List<String> {
        val timeSteps = logits.size / vocabSize
        val tokens = ArrayList<String>(timeSteps / 2)
        var prevId = -1

        for (t in 0 until timeSteps) {
            // Argmax trên vocab dimension
            val base = t * vocabSize
            var maxId = 0
            var maxVal = logits[base]
            for (v in 1 until vocabSize) {
                val cur = logits[base + v]
                if (cur > maxVal) { maxVal = cur; maxId = v }
            }

            // Collapse duplicates + bỏ special tokens
            if (maxId != prevId && !vocab.isSpecial(maxId)) {
                vocab.decode(maxId)?.let { tokens.add(it) }
            }
            prevId = maxId
        }
        return tokens
    }
}

// ─────────────────────────────────────────────
// Wav2Vec2Phoneme — ONNX inference
// ─────────────────────────────────────────────

/**
 * Wav2Vec2Phoneme — wrapper cho OnnxRuntime inference trên Android.
 *
 * Lifecycle:
 *   val recognizer = Wav2Vec2Phoneme(context)
 *   recognizer.load()           // gọi 1 lần khi app start
 *   val phonemes = recognizer.recognize(floatArray)
 *   recognizer.close()          // khi không dùng nữa
 */
class Wav2Vec2Phoneme(private val context: Context) : AutoCloseable {

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var vocab: Wav2Vec2Vocab? = null

    // ── Load model ────────────────────────────

    /**
     * Load model + vocab vào OnnxRuntime.
     * Model được tải về từ [modelUrl] và cache tại cacheDir.
     * Gọi trong coroutine hoặc background thread — blocking I/O.
     *
     * @param modelUrl      URL tải model ONNX (raw, không phải GitHub blob viewer)
     * @param modelFileName Tên file lưu trong cacheDir
     * @param vocabFileName Tên file vocab trong assets
     * @param useGPU        Dùng NNAPI nếu có
     * @param onProgress    Callback tiến trình tải (0–100). Gọi trên thread hiện tại.
     *                      Trả về 100 ngay lập tức nếu file đã cache sẵn.
     */
    fun load(
        modelUrl: String = "https://github.com/hoanganhtuan95ptit/Phonetics/raw/refs/heads/${BRANCH}/models/wav2vec2_phoneme.onnx",
        modelFileName: String = "wav2vec2_phoneme.onnx",
        vocabFileName: String = Wav2Vec2Vocab.ASSET_FILE_NAME,
        useGPU: Boolean = false,
        onProgress: ((percent: Int) -> Unit)? = null,
    ) {

        // Tải model từ URL về cacheDir, báo tiến trình qua onProgress
        val modelFile = downloadModelIfNeeded(modelUrl, modelFileName, onProgress)

        // Load vocab trước — nếu fail thì biết ngay (file thiếu trong assets)
        vocab = Wav2Vec2Vocab.loadFromAssets(context, vocabFileName)

        ortEnv = OrtEnvironment.getEnvironment()

        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(4)
            setInterOpNumThreads(1)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)

            if (useGPU) {
                try { addNnapi() } catch (_: Exception) { /* fallback CPU */ }
            }
        }

        try {
            session = ortEnv!!.createSession(modelFile.absolutePath, sessionOptions)
        } catch (t: Throwable) {
            Log.d("tuanha", "load: ", t)
            modelFile.delete()
            throw t
        }
    }

    /**
     * Tải model từ [url] về cacheDir nếu chưa có (hoặc file rỗng/lỗi).
     * Dùng file .tmp để tránh để lại file incomplete khi bị gián đoạn.
     *
     * @param onProgress Callback (0–100). Không gọi nếu server không trả Content-Length.
     *                   Luôn gọi với 100 khi hoàn thành.
     */
    private fun downloadModelIfNeeded(
        url: String,
        fileName: String,
        onProgress: ((Int) -> Unit)?
    ): File {
        val outFile = File(context.cacheDir, fileName)

        // File hợp lệ — bỏ qua download
        if (outFile.exists() && outFile.length() > 0) {
            onProgress?.invoke(100)
            return outFile
        }

        val tempFile = File(context.cacheDir, "$fileName.tmp")
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.connect()

            val contentLength = connection.contentLengthLong   // -1 nếu không có header

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var lastProgress = -1

                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                        downloaded += n

                        if (onProgress != null && contentLength > 0) {
                            val progress = (downloaded * 100L / contentLength).toInt().coerceIn(0, 99)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                    output.fd.sync()
                }
            }

            tempFile.renameTo(outFile)
            onProgress?.invoke(100)
            return outFile

        } catch (t: Throwable) {
            tempFile.delete()
            throw t
        }
    }

    // ── Inference ─────────────────────────────

    /**
     * Nhận dạng phoneme từ audio.
     *
     * @param audioFloat  PCM float32, 16kHz, mono. Phải đã normalize
     *                    zero-mean / unit-variance (xem PronunciationPipeline).
     * @return list IPA phonemes, ví dụ ["h", "ɛ", "l", "oʊ"]
     */
    fun recognize(audioFloat: FloatArray): List<String> {
        val env     = ortEnv     ?: error("Chưa gọi load()")
        val session = this.session ?: error("Chưa gọi load()")
        val vocab   = this.vocab   ?: error("Chưa gọi load() — vocab chưa load")

        // Input tensor shape [1, num_samples]
        val inputShape  = longArrayOf(1, audioFloat.size.toLong())
        OnnxTensor.createTensor(env, FloatBuffer.wrap(audioFloat), inputShape).use { inputTensor ->

            // Run inference (dùng use{} để close tự động → tránh leak GPU/CPU buffer)
            session.run(mapOf("input_values" to inputTensor)).use { results ->

                // Lấy output 'logits'
                val logitsTensor = (results.firstOrNull { it.key == "logits" }?.value as? OnnxTensor)
                    ?: error("Output 'logits' not found")

                val shape = logitsTensor.info.shape  // [1, time_steps, vocab_size]
                val totalElements = shape.fold(1L) { acc, l -> acc * l }.toInt()
                val logitsFlat = FloatArray(totalElements)
                logitsTensor.floatBuffer.get(logitsFlat)

                val vocabSize = shape[2].toInt()
                check(vocabSize == vocab.size) {
                    "Vocab size mismatch: model=$vocabSize  asset=${vocab.size}. " +
                    "Bạn đang dùng vocab.json không khớp model."
                }

                return CTCDecoder.greedyDecode(logitsFlat, vocabSize, vocab)
            }
        }
    }

    override fun close() {
        session?.close()
        ortEnv?.close()
        session = null
        ortEnv = null
        vocab = null
    }
}

/*
 * ─────────────────────────────────────────────
 * Script export ONNX + vocab.json
 * ─────────────────────────────────────────────
 *
 * Xem tools/export_wav2vec2_onnx.py — script đó vừa export ONNX,
 * vừa dump vocab.json từ chính Wav2Vec2PhonemeCTCTokenizer.
 *
 * Sau khi chạy script, copy 2 file vào assets/:
 *   - wav2vec2_phoneme.onnx
 *   - wav2vec2_phoneme_vocab.json
 *
 * Đây là cách DUY NHẤT đảm bảo index → IPA token đúng. Mọi cách
 * hardcode bảng tokens trong Kotlin đều sẽ lệch — vì
 * Wav2Vec2PhonemeCTCTokenizer có thứ tự riêng (n=4, s=5, t=6, ə=7,
 * l=8, a=9...) không phải thứ tự IPA chuẩn nào.
 */
