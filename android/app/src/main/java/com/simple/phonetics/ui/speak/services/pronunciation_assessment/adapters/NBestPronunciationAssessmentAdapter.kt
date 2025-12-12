package com.simple.phonetics.ui.speak.services.pronunciation_assessment.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setText
import com.simple.phonetics.databinding.ItemPronunciationAssessmentNbestBinding

@ItemAdapter
class NBestPronunciationAssessmentAdapter : ViewItemAdapter<NBestPronunciationAssessmentViewItem, ItemPronunciationAssessmentNbestBinding>() {

    override val viewItemClass: Class<NBestPronunciationAssessmentViewItem> by lazy {
        NBestPronunciationAssessmentViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemPronunciationAssessmentNbestBinding {
        return ItemPronunciationAssessmentNbestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemPronunciationAssessmentNbestBinding, viewType: Int, position: Int, item: NBestPronunciationAssessmentViewItem, payloads: MutableList<Any>) {

        binding.pronScore.progress = item.pronScore

        binding.tvPron.setText(item.pron)
        binding.tvFluency.setText(item.fluency)
        binding.tvAccuracy.setText(item.accuracy)
        binding.tvCompleteness.setText(item.completeness)
    }

    override fun onBindViewHolder(binding: ItemPronunciationAssessmentNbestBinding, viewType: Int, position: Int, item: NBestPronunciationAssessmentViewItem) {

        binding.pronScore.progress = item.pronScore

        binding.tvPron.setText(item.pron)
        binding.tvFluency.setText(item.fluency)
        binding.tvAccuracy.setText(item.accuracy)
        binding.tvCompleteness.setText(item.completeness)
    }
}

data class NBestPronunciationAssessmentViewItem(

    val id: String = "",

    val pronScore: Float,

    val pron: RichText = emptyText(),
    val fluency: RichText = emptyText(),
    val accuracy: RichText = emptyText(),
    val completeness: RichText = emptyText()
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(id)

    override fun getContentsCompare(): List<Pair<Any, String>> = arrayListOf(
        pronScore to "pronScore"
    )
}