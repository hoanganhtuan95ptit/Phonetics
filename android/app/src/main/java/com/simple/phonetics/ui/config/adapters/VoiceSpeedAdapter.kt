package com.simple.phonetics.ui.config.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.databinding.ItemVoiceSpeedBinding

@ItemAdapter
class VoiceSpeedAdapter(private val onItemClick: (View, VoiceSpeedViewItem) -> Unit = { _, _ -> }) : ViewItemAdapter<VoiceSpeedViewItem, ItemVoiceSpeedBinding>() {

    override val viewItemClass: Class<VoiceSpeedViewItem> by lazy {
        VoiceSpeedViewItem::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemVoiceSpeedBinding {
        return ItemVoiceSpeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun onBindViewHolder(binding: ItemVoiceSpeedBinding, viewType: Int, position: Int, item: VoiceSpeedViewItem, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        if (payloads.contains(PAYLOAD_CURRENT)) {

            refreshCurrent(binding, item)
        }
    }

    override fun onBindViewHolder(binding: ItemVoiceSpeedBinding, viewType: Int, position: Int, item: VoiceSpeedViewItem) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.progress.max = (item.end * 100).toInt()

        binding.progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

                val item = getViewItem(position) ?: return

                val current = p1 / 100.0f

                if (item.current == current) return

                item.current = current

                onItemClick(binding.root, item)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        refreshCurrent(binding, item)
    }

    private fun refreshCurrent(binding: ItemVoiceSpeedBinding, item: VoiceSpeedViewItem) {

        binding.progress.progress = (item.current * 100).toInt()
    }
}

data class VoiceSpeedViewItem(
    val end: Float,
    val start: Float,

    val text: String,

    var current: Float
) : ViewItem {

    override fun areItemsTheSame(): List<Any> = listOf(
        end, start
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        current to PAYLOAD_CURRENT
    )
}

private const val PAYLOAD_CURRENT = "PAYLOAD_CURRENT"
