package com.simple.phonetics.ui.speak.services.pronunciation_assessment.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.setText
import com.simple.phonetics.databinding.ItemPronunciationAssessmentWordBinding

@ItemAdapter
class WordPronunciationAssessmentAdapter : ViewItemAdapter<WordPronunciationAssessmentViewItem, ItemPronunciationAssessmentWordBinding>() {

    override val viewItemClass: Class<WordPronunciationAssessmentViewItem> by lazy {
        WordPronunciationAssessmentViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemPronunciationAssessmentWordBinding {
        return ItemPronunciationAssessmentWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemPronunciationAssessmentWordBinding, viewType: Int, position: Int, item: WordPronunciationAssessmentViewItem, payloads: MutableList<Any>) {

        binding.tvWord.setText(item.word)
        binding.tvPhonetic.setText(item.phonetic)

        binding.root.delegate.strokeColor = item.backgroundStrokeColor
        binding.root.delegate.setBgSelector()
    }

    override fun onBindViewHolder(binding: ItemPronunciationAssessmentWordBinding, viewType: Int, position: Int, item: WordPronunciationAssessmentViewItem) {

        binding.tvWord.setText(item.word)
        binding.tvPhonetic.setText(item.phonetic)

        binding.root.delegate.strokeColor = item.backgroundStrokeColor
        binding.root.delegate.setBgSelector()
    }
}

data class WordPronunciationAssessmentViewItem(

    val id: String = "",

    val word: RichText = emptyText(),
    val phonetic: RichText = emptyText(),

    val backgroundStrokeColor: Int = Color.TRANSPARENT
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(id)

    override fun getContentsCompare(): List<Pair<Any, String>> = arrayListOf(
        word to "word",
        phonetic to "phonetic",
        backgroundStrokeColor to "backgroundStrokeColor"
    )
}