package com.simple.feature.pronunciation_assessment.domain.repositories

import android.Manifest
import androidx.annotation.RequiresPermission
import com.simple.feature.pronunciation_assessment.data.audio.AudioRecorderImpl
import com.simple.feature.pronunciation_assessment.data.dictionary.PhonemeDictionaryImpl
import com.simple.feature.pronunciation_assessment.data.repositories.PronunciationAssessmentRepositoryImpl
import com.simple.feature.pronunciation_assessment.domain.entities.AssessmentEvent
import com.simple.feature.pronunciation_assessment.domain.entities.AssessmentState
import com.simple.phonetics.PhoneticsApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Orchestrator chấm phát âm — gắn [AudioRecorder] + [PhonemeRecognizer] +
 * scorer thành một pipeline hoàn chỉnh.
 */
interface PronunciationAssessmentRepository : AutoCloseable {

    /** Trạng thái pipeline hiện tại. */
    val state: StateFlow<AssessmentState>

    /** Câu reference dưới dạng list (word, IPA phonemes). */
    val referenceWords: List<Pair<String, List<String>>>

    /** Text reference đã ghép, tiện cho hiển thị. */
    val referenceText: String

    /**
     * Load model + set câu reference. Gọi 1 lần khi khởi tạo.
     *
     * @param reference  danh sách cặp (word, IPA phonemes)
     * @param useGPU     dùng NNAPI nếu có
     * @param onProgress callback tiến trình tải model (0–100)
     */
    suspend fun prepare(
        reference: List<Pair<String, List<String>>>,
        useGPU: Boolean = false,
        onProgress: ((percent: Int) -> Unit)? = null,
    )

    /** Đổi reference mà không cần load lại model. */
    fun setReference(reference: List<Pair<String, List<String>>>)

    /**
     * Bắt đầu nghe + chấm phát âm. Trả về Flow phát [AssessmentEvent].
     *
     * Flow tự đóng khi:
     *   - VAD phát hiện người dùng dừng nói (Final emit xong)
     *   - Có lỗi (Error emit xong)
     *   - Collector cancel — pipeline sẽ dừng mic và worker
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(): Flow<AssessmentEvent>

    /** Dừng pipeline thủ công (không chờ VAD). */
    fun stop()

    fun release()

    companion object {

        // ── Manual wiring (data layer instantiated trên-tay theo yêu cầu) ───
        val instance by lazy {
            val ctx = PhoneticsApp.share
            PronunciationAssessmentRepositoryImpl(
                audioRecorder = AudioRecorderImpl(ctx),
                phonemeRecognizer = PhonemeRecognizer.instance,
                phonemeDictionary = PhonemeDictionaryImpl.load(ctx),
            )
        }
    }
}
