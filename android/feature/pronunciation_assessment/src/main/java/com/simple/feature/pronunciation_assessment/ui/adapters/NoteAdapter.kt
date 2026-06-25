package com.simple.feature.pronunciation_assessment.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.emptyText
import com.simple.coreapp.utils.ext.resize
import com.simple.coreapp.utils.ext.setText
import com.simple.feature.pronunciation_assessment.databinding.PronunciationAssessmentItemNoteBinding
import com.simple.image.RichImage
import com.simple.image.emptyImage
import com.simple.image.setImage
import com.simple.phonetics.Payload

@ItemAdapter
class NoteAdapter : ViewItemAdapter<NoteViewItem, PronunciationAssessmentItemNoteBinding>() {

    override val viewItemClass: Class<NoteViewItem> by lazy {
        NoteViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): PronunciationAssessmentItemNoteBinding {
        val binding = PronunciationAssessmentItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return binding
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<PronunciationAssessmentItemNoteBinding> {
        return BaseBindingViewHolder(createViewBinding(parent, viewType), viewType)
    }

    override fun onBindViewHolder(binding: PronunciationAssessmentItemNoteBinding, viewType: Int, position: Int, item: NoteViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(Payload.TEXT)) {
            binding.tvNote.setText(item.note)
        }

        if (payloads.contains(Payload.IMAGE)) {
            binding.ivNote.setImage(item.image)
        }

        if (payloads.contains(Payload.BACKGROUND)) {
            binding.root.setBackground(item.background)
        }
    }

    override fun onBindViewHolder(binding: PronunciationAssessmentItemNoteBinding, viewType: Int, position: Int, item: NoteViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        Log.d("tuanha", "onBindViewHolder: ")
        binding.tvNote.setText(item.note)
        binding.ivNote.setImage(item.image)

        binding.root.setBackground(item.background)
    }
}

data class NoteViewItem(
    val id: String,

    val note: RichText = emptyText(),
    val image: RichImage = emptyImage(),

    val background: Background = Background()
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        note to Payload.TEXT,
        image to Payload.IMAGE,
        background to Payload.BACKGROUND
    )
}
