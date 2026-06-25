package com.simple.phonetics.ui.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.permissionx.guolindev.PermissionX
import com.simple.core.utils.extentions.asObject
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.phonetics.R
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.speak.services.pronunciation_assessment.PronunciationViewModel
import com.simple.phonetics.ui.view.outline.OutlineFrameLayout
import com.simple.state.doRunning
import com.simple.state.isSuccess
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.Job

class PronunciationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : OutlineFrameLayout(context, attrs) {

    var sentences: List<Sentence> = emptyList()

    private var tvAction: TextView
    private var ivRecord: RecordingWaveView
    private var progressPronunciation: ProgressBar

    private val viewModel: PronunciationViewModel by lazy {
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!)[PronunciationViewModel::class.java]
    }

    init {
        inflate(context, R.layout.layout_pronunciation, this)

        tvAction = findViewById(R.id.tv_action)
        ivRecord = findViewById(R.id.recordingWaveView)
        progressPronunciation = findViewById(R.id.progressPronunciation)


        var job: Job? = null

        setDebouncedClickListener {

            job?.cancel()

            if (!viewModel.initState.value.isSuccess()) {

                job = viewModel.loadModel(sentences)
            } else PermissionX.init(findViewTreeViewModelStoreOwner().asObject<Fragment>()).permissions(arrayOf(Manifest.permission.RECORD_AUDIO).toList()).request { allGranted, _, _ ->

                @SuppressLint("MissingPermission")
                if (allGranted) job = viewModel.record()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

        viewModel.themes.launchCollect(lifecycleOwner) {

            strokeWidth = DP.DP_1.toFloat()
            strokeColor = it.colorPrimary
        }

        viewModel.buttonData.asFlow().launchCollect(lifecycleOwner) {

            setLoading(it.loading, animate = true)

            isClickable = !it.loading

            tvAction.animate().alpha(if (it.textShow) 1f else 0f).start()
            tvAction.setText(it.text)

            progressPronunciation.animate().alpha(if (it.progressShow) 0.5f else 0f).start()

            ivRecord.animate().alpha(if (it.imageShow) 1f else 0f).start()
            if (it.imageShow) {
                ivRecord.startRecording()
            } else {
                ivRecord.stopRecording()
            }

            setBackground(it.background)
        }

        viewModel.initState.asFlow().launchCollect(lifecycleOwner) {

            it.doRunning {
                progressPronunciation.progress = it
            }
        }
    }
}