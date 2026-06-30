package com.simple.feature.pronunciation_assessment.domain.repositories

import android.Manifest
import androidx.annotation.RequiresPermission
import com.simple.feature.pronunciation_assessment.data.audio.AudioRecorderImpl
import com.simple.feature.pronunciation_assessment.data.dictionary.PhonemeDictionaryImpl
import com.simple.feature.pronunciation_assessment.data.repositories.PronunciationAssessmentRepositoryImpl
import com.simple.feature.pronunciation_assessment.domain.entities.AssessmentEvent
import com.simple.phonetics.PhoneticsApp
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

/**
 * Orchestrator chấm phát âm — gắn [AudioRecorder] + [PhonemeRecognizer] +
 * scorer thành một pipeline hoàn chỉnh.
 */
interface PronunciationAssessmentRepository : AutoCloseable {

    /**
     * Load model. Gọi 1 lần khi khởi tạo và phát tiến trình tải model.
     *
     * @param useGPU dùng NNAPI nếu có
     * @return Flow tiến trình 0-100; lỗi load model được throw qua Flow.
     */
    fun prepare(useGPU: Boolean = false): Flow<ResultState<Int>>

    /**
     * Bắt đầu nghe + chấm phát âm. Trả về Flow phát [AssessmentEvent].
     *
     * @param referenceWords danh sách cặp (word, IPA phonemes)
     *
     * Flow tự đóng khi:
     *   - VAD phát hiện người dùng dừng nói (Final emit xong)
     *   - Có lỗi (Error emit xong)
     *   - Collector cancel — pipeline sẽ dừng mic và worker
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(referenceWords: List<Pair<String, List<String>>>): Flow<AssessmentEvent>

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
