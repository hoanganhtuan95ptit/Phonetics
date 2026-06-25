package com.simple.phonetics.ui.speak.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.databinding.ItemScoreResultBinding
import com.simple.phonetics.entities.SentenceScore

@ItemAdapter
class ScoreResultAdapter(
    onItemClick: (View, ScoreResultViewItem) -> Unit = { _, _ -> }
) : ViewItemAdapter<ScoreResultViewItem, ItemScoreResultBinding>(onItemClick) {

    override val viewItemClass: Class<ScoreResultViewItem> by lazy {
        ScoreResultViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemScoreResultBinding {
        return ItemScoreResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(
        binding: ItemScoreResultBinding,
        viewType: Int,
        position: Int,
        item: ScoreResultViewItem,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_SCORE)) refreshScore(binding, item)
        if (payloads.contains(PAYLOAD_SUBTITLE)) refreshSubtitle(binding, item)
        if (payloads.contains(PAYLOAD_METRICS)) refreshMetrics(binding, item)
    }

    override fun onBindViewHolder(
        binding: ItemScoreResultBinding,
        viewType: Int,
        position: Int,
        item: ScoreResultViewItem
    ) {
        super.onBindViewHolder(binding, viewType, position, item)

        refreshScore(binding, item)
        refreshSubtitle(binding, item)
        refreshMetrics(binding, item)
    }

    private fun refreshScore(binding: ItemScoreResultBinding, item: ScoreResultViewItem) {
        binding.scoreGaugeView.progress = item.score
        binding.scoreGaugeView.grade = item.grade
    }

    private fun refreshSubtitle(binding: ItemScoreResultBinding, item: ScoreResultViewItem) {
        binding.tvSubtitle.text = item.subtitle
    }

    private fun refreshMetrics(binding: ItemScoreResultBinding, item: ScoreResultViewItem) {
        binding.tvAccuracyValue.text = "${item.accuracy}%"
        binding.progressAccuracy.progress = item.accuracy

        binding.tvCompletionValue.text = "${item.completion}%"
        binding.progressCompletion.progress = item.completion

        binding.tvFluencyValue.text = "${item.fluency}%"
        binding.progressFluency.progress = item.fluency
    }
}

data class ScoreResultViewItem(
    val id: String,
    val data: SentenceScore,

    val score: Int = data.finalScore,
    val grade: String = gradeOf(data.finalScore),
    val subtitle: String = "",

    val accuracy: Int = data.accuracyScore,
    val completion: Int = data.completenessScore,
    val fluency: Int = (100 - data.fluencyPenalty).coerceAtLeast(0),
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

fun gradeOf(score: Int): String = when {
    score >= 90 -> "GRADE A"
    score >= 80 -> "GRADE B+"
    score >= 70 -> "GRADE B"
    score >= 60 -> "GRADE C+"
    score >= 50 -> "GRADE C"
    else        -> "GRADE D"
}

private const val PAYLOAD_SCORE    = "PAYLOAD_SCORE"
private const val PAYLOAD_SUBTITLE = "PAYLOAD_SUBTITLE"
private const val PAYLOAD_METRICS  = "PAYLOAD_METRICS"
