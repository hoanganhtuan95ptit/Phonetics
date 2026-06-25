package com.simple.feature.pronunciation_assessment.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setText
import com.simple.feature.pronunciation_assessment.databinding.PronunciationAssessmentItemScoreResultBinding
import com.simple.phonetics.entities.SentenceScore

@ItemAdapter
class ScoreResultAdapter : ViewItemAdapter<ScoreResultViewItem, PronunciationAssessmentItemScoreResultBinding>() {

    override val viewItemClass: Class<ScoreResultViewItem> by lazy {
        ScoreResultViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): PronunciationAssessmentItemScoreResultBinding {
        return PronunciationAssessmentItemScoreResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: PronunciationAssessmentItemScoreResultBinding, viewType: Int, position: Int, item: ScoreResultViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_SCORE)) refreshScore(binding, item)
        if (payloads.contains(PAYLOAD_SUBTITLE)) refreshSubtitle(binding, item)
        if (payloads.contains(PAYLOAD_METRICS)) refreshMetrics(binding, item)
    }

    override fun onBindViewHolder(binding: PronunciationAssessmentItemScoreResultBinding, viewType: Int, position: Int, item: ScoreResultViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        refreshScore(binding, item)
        refreshSubtitle(binding, item)
        refreshMetrics(binding, item)
    }

    private fun refreshScore(binding: PronunciationAssessmentItemScoreResultBinding, item: ScoreResultViewItem) {
        binding.scoreGaugeView.label = item.label
        binding.scoreGaugeView.grade = item.grade
        binding.scoreGaugeView.progress = item.score
    }

    private fun refreshSubtitle(binding: PronunciationAssessmentItemScoreResultBinding, item: ScoreResultViewItem) {
        binding.tvSubtitle.setText(item.subtitle)
    }

    private fun refreshMetrics(binding: PronunciationAssessmentItemScoreResultBinding, item: ScoreResultViewItem) {
        binding.tvAccuracyTitle.setText(item.accuracyTitle)
        binding.tvAccuracyValue.setText(item.accuracyValue)
        binding.progressAccuracy.progress = item.accuracy

        binding.tvCompletionTitle.setText(item.completionTitle)
        binding.tvCompletionValue.setText(item.completionValue)
        binding.progressCompletion.progress = item.completion

        binding.tvFluencyTitle.setText(item.fluencyTitle)
        binding.tvFluencyValue.setText(item.fluencyValue)
        binding.progressFluency.progress = item.fluency
    }
}

data class ScoreResultViewItem(
    val id: String,

    val score: Int = 0,
    val label: String = "",
    val grade: String = "",
    val subtitle: RichText = emptyText(),

    val accuracy: Int = 0,
    val accuracyTitle: RichText = emptyText(),
    val accuracyValue: RichText = emptyText(),

    val completion: Int = 0,
    val completionTitle: RichText = emptyText(),
    val completionValue: RichText = emptyText(),

    val fluency: Int = 0,
    val fluencyTitle: RichText = emptyText(),
    val fluencyValue: RichText = emptyText(),
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(id)

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        score to PAYLOAD_SCORE,
        grade to PAYLOAD_SCORE,
        subtitle to PAYLOAD_SUBTITLE,
        accuracy to PAYLOAD_METRICS,
        completion to PAYLOAD_METRICS,
        fluency to PAYLOAD_METRICS,
    )
}

private const val PAYLOAD_SCORE    = "PAYLOAD_SCORE"
private const val PAYLOAD_SUBTITLE = "PAYLOAD_SUBTITLE"
private const val PAYLOAD_METRICS  = "PAYLOAD_METRICS"
