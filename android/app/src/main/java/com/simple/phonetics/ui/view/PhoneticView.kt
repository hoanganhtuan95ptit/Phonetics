package com.simple.phonetics.ui.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.phonetics.domain.usecase.reading.StartReadingUseCase
import com.simple.phonetics.domain.usecase.reading.StopReadingUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.view.outline.OutlineDelegate
import com.simple.phonetics.ui.view.outline.OutlineHost
import com.simple.phonetics.utils.exts.value
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isStart
import com.simple.ui.precompute.PrecomputedDelegate
import com.simple.ui.precompute.PrecomputedHost
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PhoneticView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), OutlineHost, PrecomputedHost {

    var id: String = ""
    var text: String = ""


    var hasStroke: Boolean = false


    override val outline = OutlineDelegate(this, context, attrs)

    override val delegate: PrecomputedDelegate = PrecomputedDelegate(this, context, attrs)


    private val viewModel: ReadingViewModel by lazy {
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!)[ReadingViewModel::class.java]
    }


    init {

        setDebouncedClickListener {

            viewModel.startReading(id, text)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outline.onSizeChanged(w, h)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(delegate.spec?.width ?: 0, delegate.spec?.height ?: 0)
    }

    override fun onDraw(canvas: Canvas) {
        outline.onDraw(canvas)
        delegate.onDraw(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        outline.onAttachedToWindow()
        delegate.onAttachedToWindow()

        val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

        viewModel.readingState.launchCollect(lifecycleOwner) {

            val state = if (it.first == id) {
                it.second
            } else {
                ResultState.Idle
            }

            setLoading(state.isStart(), show = hasStroke || state.isStart(), animate = true)

            isClickable = !state.isStart() && viewModel.isSupportReadingFlow.value == true
        }

        viewModel.isSupportReadingFlow.launchCollect(lifecycleOwner) {

            isClickable = it
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        outline.onDetachedFromWindow()
        delegate.onDetachedFromWindow()
    }
}

class ReadingViewModel : BaseViewModel() {

    val readingState: MutableStateFlow<Pair<String, ResultState<String>>> = MutableStateFlow("" to ResultState.Idle)

    fun startReading(id: String = "", text: String) = launchWithTag("reading") {

        readingState.value = (id to ResultState.Start)

        val param = StartReadingUseCase.Param(
            text = text
        )

        var job: Job? = null
        job = StartReadingUseCase.instant.execute(param).launchCollect(viewModelScope) { state ->

            readingState.value = (id to state)

            state.doSuccess {
                job?.cancel()
            }

            state.doFailed {
                job?.cancel()
            }
        }
    }

    fun stopReading() = viewModelScope.launch(handler + Dispatchers.IO) {

        StopReadingUseCase.instant.execute()
    }
}
