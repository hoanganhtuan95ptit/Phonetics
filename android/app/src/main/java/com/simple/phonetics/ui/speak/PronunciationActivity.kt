package com.simple.phonetics.ui.speak

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED as GRANTED
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.simple.phonetics.R
import com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case.*
import kotlinx.coroutines.launch

class PronunciationActivity : AppCompatActivity() {

    private lateinit var pipeline: PronunciationPipeline

    private lateinit var statusText: TextView
    private lateinit var referenceText: TextView
    private lateinit var wordsContainer: LinearLayout
    private lateinit var errorsCard: LinearLayout
    private lateinit var errorsContainer: LinearLayout
    private lateinit var micButton: Button

    // metric cards (each is the <include> root LinearLayout)
    private lateinit var metricAccuracy: View
    private lateinit var metricCompleteness: View
    private lateinit var metricFluency: View
    private lateinit var metricFinal: View

    // Reference dạng list cặp (word, IPA) — caller tự lookup từ dict đầy đủ
    private val reference: List<Pair<String, List<String>>> = listOf(
        "the" to listOf("ð", "ə"),
        "cat" to listOf("k", "æ", "t"),
        "sat" to listOf("s", "æ", "t"),
        "on"  to listOf("ɒ", "n"),
        "the" to listOf("ð", "ə"),
        "mat" to listOf("m", "æ", "t"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pronunciation)

        statusText      = findViewById(R.id.statusText)
        referenceText   = findViewById(R.id.referenceText)
        wordsContainer  = findViewById(R.id.wordsContainer)
        errorsCard      = findViewById(R.id.errorsCard)
        errorsContainer = findViewById(R.id.errorsContainer)
        micButton       = findViewById(R.id.micButton)

        metricAccuracy     = findViewById(R.id.metricAccuracy)
        metricCompleteness = findViewById(R.id.metricCompleteness)
        metricFluency      = findViewById(R.id.metricFluency)
        metricFinal        = findViewById(R.id.metricFinal)

        referenceText.text = "\"" + reference.joinToString(" ") { it.first } + "\""

        // Initial metric labels + empty values
        setMetric(metricAccuracy,     "Accuracy",     "–")
        setMetric(metricCompleteness, "Completeness", "–")
        setMetric(metricFluency,      "Fluency −",    "–")
        setMetric(metricFinal,        "Điểm cuối",    "–", isFinal = true)

        pipeline = PronunciationPipeline(this)

        // Load model
        lifecycleScope.launch {
            pipeline.prepare(reference = reference, useGPU = true)
        }

        pipeline.onPartialResult = { score -> renderScore(score) }
        pipeline.onFinalResult   = { score -> renderScore(score); score.prettyPrint() }

        pipeline.onStateChange = { state ->
            micButton.isActivated = (state == PipelineState.RECORDING)
            statusText.text = when (state) {
                PipelineState.READY      -> "Sẵn sàng"
                PipelineState.LISTENING  -> "Đang nghe..."
                PipelineState.RECORDING  -> "Đang ghi âm"
                PipelineState.PROCESSING -> "Đang xử lý..."
                PipelineState.ERROR      -> "Lỗi"
                else                     -> "Đang tải model..."
            }
            micButton.text = when (state) {
                PipelineState.RECORDING, PipelineState.LISTENING -> "Dừng lại"
                else -> "Bắt đầu nói"
            }
        }

        pipeline.onError = { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        micButton.setOnClickListener {
            when (pipeline.state) {
                PipelineState.READY -> {
                    if (checkSelfPermission(RECORD_AUDIO) == GRANTED) pipeline.startListening()
                    else requestPermissions(arrayOf(RECORD_AUDIO), 1)
                }
                PipelineState.RECORDING,
                PipelineState.LISTENING -> pipeline.stopListening()
                else -> {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pipeline.close()
    }

    // ─────────────────────────────────────────────
    // Render score → UI
    // ─────────────────────────────────────────────

    private fun renderScore(score: SentenceScore) {
        // 1. Word cards
        wordsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        score.wordScores.forEach { ws ->
            val row = inflater.inflate(R.layout.item_word_score, wordsContainer, false)
            row.findViewById<TextView>(R.id.wordText).text = ws.word
            row.findViewById<TextView>(R.id.wordScoreText).text = "${ws.score} / 100"

            // Progress fill width via weight trick: width fraction
            val fill = row.findViewById<View>(R.id.progressFill)
            fill.post {
                val parent = fill.parent as View
                val parentWidth = parent.width
                val fraction = ws.score.coerceIn(0, 100) / 100f
                fill.layoutParams = fill.layoutParams.apply {
                    width = (parentWidth * fraction).toInt()
                }
                (fill.background as? GradientDrawable)?.setColor(colorForScore(ws.score))
                fill.requestLayout()
            }

            // Phoneme details
            val phonemesBox = row.findViewById<LinearLayout>(R.id.phonemesContainer)
            ws.phonemeScores.forEach { ps ->
                phonemesBox.addView(buildPhonemeRow(ps))
            }

            wordsContainer.addView(row)
        }

        // 2. Metrics
        setMetric(metricAccuracy,     "Accuracy",     "${score.accuracyScore} / 100")
        setMetric(metricCompleteness, "Completeness", "${score.completenessScore}%")
        setMetric(metricFluency,      "Fluency −",    "${score.fluencyPenalty}")
        setMetric(metricFinal,        "Điểm cuối",    "${score.finalScore} / 100", isFinal = true)

        // 3. Errors
        errorsContainer.removeAllViews()
        if (score.errors.isEmpty()) {
            errorsCard.isVisible = false
        } else {
            errorsCard.isVisible = true
            score.errors.forEach { e ->
                errorsContainer.addView(buildErrorRow(e))
            }
        }
    }

    private fun buildPhonemeRow(ps: PhonemeScore): TextView {
        val tv = TextView(this).apply {
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 2, 0, 2)
        }
        val (mark, color) = when (ps.errorType) {
            ErrorType.CORRECT      -> "✓" to 0xFF0F6E56.toInt()
            ErrorType.SUBSTITUTION -> "✗" to 0xFFA32D2D.toInt()
            ErrorType.DELETION     -> "∅" to 0xFFA32D2D.toInt()
            ErrorType.INSERTION    -> "+" to 0xFFBA7517.toInt()
        }
        val suffix = when (ps.errorType) {
            ErrorType.SUBSTITUTION -> "  →  /${ps.actual}/"
            ErrorType.DELETION     -> "  (bị nuốt)"
            else                   -> ""
        }
        tv.text = "$mark  /${ps.expected}/   ${ps.score}/100$suffix"
        tv.setTextColor(color)
        return tv
    }

    private fun buildErrorRow(e: PronunciationError): TextView {
        val msg = when (e.errorType) {
            ErrorType.SUBSTITUTION -> "• /${e.phoneme}/ đọc thành /${e.substitutedWith}/  (\"${e.wordContext}\")"
            ErrorType.DELETION     -> "• /${e.phoneme}/ bị nuốt  (\"${e.wordContext}\")"
            ErrorType.INSERTION    -> "• /${e.phoneme}/ thêm thừa"
            else                   -> ""
        }
        return TextView(this).apply {
            text = msg
            textSize = 13f
            setTextColor(0xFF444441.toInt())
            setPadding(0, 3, 0, 3)
        }
    }

    private fun setMetric(card: View, label: String, value: String, isFinal: Boolean = false) {
        card.findViewById<TextView>(R.id.metricLabel).apply {
            text = label
            if (isFinal) setTextColor(0xFFA32D2D.toInt())
        }
        card.findViewById<TextView>(R.id.metricValue).apply {
            text = value
            if (isFinal) setTextColor(0xFF501313.toInt())
        }
        if (isFinal) card.setBackgroundResource(R.drawable.bg_metric_final)
    }

    private fun colorForScore(s: Int): Int = when {
        s >= 80 -> 0xFF1D9E75.toInt()  // teal/green
        s >= 60 -> 0xFFEF9F27.toInt()  // amber
        else    -> 0xFFE24B4A.toInt()  // red
    }
}
