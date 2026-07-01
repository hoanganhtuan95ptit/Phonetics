package com.simple.phonetics.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.phonetics.R
import com.simple.phonetics.ui.view.outline.OutlineLinearLayout
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.isIdle
import com.simple.state.isStart
import com.simple.ui.precompute.image.setImage
import com.simple.ui.precompute.image.ColorFilter
import com.simple.ui.precompute.image.addTransform
import com.simple.ui.precompute.image.build
import com.simple.ui.precompute.image.toBuilder
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.flow.first

class TextToSpeechView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : OutlineLinearLayout(context, attrs) {

    val id: String = ""
    var text: String = ""

    private var ivSource: ImageView
    private var ivReading: ImageView

    private val viewModel: ReadingViewModel by lazy {
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!)[ReadingViewModel::class.java]
    }

    init {
        inflate(context, R.layout.layout_play, this)

        ivSource = findViewById(R.id.iv_source)
        ivSource.setImageResource(R.drawable.img_ai)

        ivReading = findViewById(R.id.iv_play_or_pause)


        setDebouncedClickListener {

            viewModel.startReading(id, text)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

        viewModel.themes.launchCollect(lifecycleOwner) {

            ivReading.setImage(
                R.drawable.ic_volume_24dp.toBuilder()
                    .addTransform(ColorFilter(it.colorPrimary))
                    .build()
            )
            strokeColor =  it.colorPrimary
        }

        viewModel.readingState.launchCollect(lifecycleOwner) {

            val state = if (it.first == id) {
                it.second
            } else {
                ResultState.Idle
            }

            setLoading(state.isStart(), animate = true)

            isClickable = !state.isStart()

            val theme = viewModel.themes.first()

            val res = if (state.isIdle() || state.isStart() || state.isCompleted()) {
                R.drawable.ic_volume_24dp.toBuilder()
                    .addTransform(ColorFilter(theme.colorPrimary))
                    .build()
            } else {
                R.drawable.ic_pause_24dp.toBuilder()
                    .addTransform(ColorFilter(theme.colorPrimary))
                    .build()
            }
            ivReading.setImage(res)
        }
    }
}
