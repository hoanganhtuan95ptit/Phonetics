package com.simple.phonetics.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.image.ImageRes
import com.simple.image.setImage
import com.simple.phonetics.R
import com.simple.phonetics.ui.view.outline.OutlineLinearLayout
import com.simple.state.isCompleted
import com.simple.state.isIdle
import com.simple.state.isStart
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first

class TextToSpeechView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : OutlineLinearLayout(context, attrs) {

    var text: String = ""

    private var ivSource: ImageView
    private var ivReading: ImageView

    private val viewModel: ReadingViewModel by lazy {
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!)["TextToSpeechView", ReadingViewModel::class.java]
    }

    init {
        inflate(context, R.layout.layout_play, this)

        ivSource = findViewById(R.id.iv_source)
        ivSource.setImage(R.drawable.img_ai)

        ivReading = findViewById(R.id.iv_play_or_pause)


        var job: Job? = null

        setDebouncedClickListener {

            job?.cancel()
            job = viewModel.startReading(text)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

        viewModel.themes.launchCollect(lifecycleOwner) {

            ivReading.setImage(ImageRes(R.drawable.ic_volume_24dp, colorFilter = it.colorPrimary))
            strokeColor =  it.colorPrimary
        }

        viewModel.readingState.asFlow().launchCollect(lifecycleOwner) {

            setLoading(it.isStart(), animate = true)

            isClickable = !it.isStart()

            val theme = viewModel.themes.first()

            val res = if (it.isIdle() || it.isStart() || it.isCompleted()) {
                ImageRes(data = R.drawable.ic_volume_24dp, colorFilter = theme.colorPrimary)
            } else {
                ImageRes(data = R.drawable.ic_pause_24dp, colorFilter = theme.colorPrimary)
            }
            ivReading.setImage(res)
        }
    }
}