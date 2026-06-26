package com.simple.feature.pronunciation_assessment.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.image.ImageRes
import com.simple.image.setImage
import com.simple.phonetics.R
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.view.outline.OutlineLinearLayout
import com.simple.phonetics.utils.exts.playMedia
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.isIdle
import com.simple.state.isStart
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AudioView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : OutlineLinearLayout(context, attrs) {

    var audioPath: String = ""

    private var ivSource: ImageView
    private var ivReading: ImageView

    private val viewModel: AudioViewModel by lazy {
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!)["AudioView", AudioViewModel::class.java]
    }

    init {
        inflate(context, R.layout.layout_play, this)

        ivSource = findViewById(R.id.iv_source)
        ivSource.setImage(R.drawable.img_people)

        ivReading = findViewById(R.id.iv_play_or_pause)


        var job: Job? = null

        setDebouncedClickListener {

            job?.cancel()
            job = viewModel.play(audioPath)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

        viewModel.themes.launchCollect(lifecycleOwner) {

            ivReading.setImage(ImageRes(R.drawable.ic_volume_24dp, colorFilter = it.colorPrimary))
            strokeColor =  it.colorPrimary
        }

        viewModel.playState.asFlow().launchCollect(lifecycleOwner) {

            setLoading(it.isStart(), animate = true)

            isClickable = !it.isStart()

            val theme = viewModel.themes.first()

            val res = if (it.isIdle() ||it.isStart() || it.isCompleted()) {
                ImageRes(data = R.drawable.ic_volume_24dp, colorFilter = theme.colorPrimary)
            } else {
                ImageRes(data = R.drawable.ic_pause_24dp, colorFilter = theme.colorPrimary)
            }
            ivReading.setImage(res)
        }
    }
}

class AudioViewModel : BaseViewModel() {

    val playState: LiveData<ResultState<String>> = MediatorLiveData(ResultState.Idle)

    fun play(audioPath: String): Job = viewModelScope.launch {

        if (audioPath.isEmpty()) {
            return@launch
        }

        (playState as? MediatorLiveData)?.value = ResultState.Start

        delay(500)

        (playState as? MediatorLiveData)?.value = ResultState.Running(audioPath)

        runCatching {
            playMedia(audioPath)
        }.onSuccess {
            (playState as? MediatorLiveData)?.value = ResultState.Success(audioPath)
        }.onFailure {
            (playState as? MediatorLiveData)?.value = ResultState.Failed(it)
        }
    }
}