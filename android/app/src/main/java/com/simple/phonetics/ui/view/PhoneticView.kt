package com.simple.phonetics.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.postValue
import com.simple.image.ImageRes
import com.simple.image.setImage
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.view.outline.OutlineLinearLayout
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isStart
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorOnSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhoneticView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : OutlineLinearLayout(context, attrs) {

    var id: String = ""
    var text: String = ""
    var hasStroke: Boolean = false

    var ivReading: ImageView
    var tvPhonetic: TextView

    private val viewModel: ReadingViewModel by lazy {
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!)["PhoneticLayout:$id", ReadingViewModel::class.java]
    }

    init {
        inflate(context, R.layout.layout_phonetic, this)

        ivReading = findViewById(R.id.iv_read)
        tvPhonetic = findViewById(R.id.tv_phonetic)

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

            ivReading.setImage(ImageRes(R.drawable.ic_volume_24dp, colorFilter = it.colorOnSurface))
        }

        viewModel.readingState.asFlow().launchCollect(lifecycleOwner) {

            setLoading(it.isStart(), show = hasStroke || it.isStart(), animate = true)

            isClickable = !it.isStart() && ivReading.isVisible
        }

        viewModel.isSupportReadingFlow.launchCollect(lifecycleOwner) {

            isClickable = it
            ivReading.setVisible(it)
        }
    }
}

class ReadingViewModel : BaseViewModel() {

    val readingState: LiveData<ResultState<String>> = MediatorLiveData(ResultState.Idle)

    fun startReading(text: String) = viewModelScope.launch(handler + Dispatchers.IO) {

        val param = StartReadingUseCase.Param(
            text = text
        )

        readingState.postValue(ResultState.Start)

        delay(1000)

        var job: Job? = null
        job = StartReadingUseCase.instant.execute(param).launchCollect(viewModelScope) { state ->

            readingState.postValue(state)

            state.doSuccess {
                job?.cancel()
            }

            state.doFailed {
                job?.cancel()
            }
        }
    }
}
