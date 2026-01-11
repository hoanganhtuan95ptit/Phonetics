package com.simple.phonetics.ui.home.services.input

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.text.layoutDirection
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.combineSourcesWithDiff
import com.simple.phonetics.utils.exts.get
import com.simple.phonetics.utils.exts.mutableSharedFlowWithDiff
import kotlinx.coroutines.flow.filterNotNull
import java.util.Locale

class InputHomeViewModel(
) : BaseViewModel() {

    @VisibleForTesting
    val isReverse = mutableSharedFlowWithDiff {
        emit(false)
    }

    val textDirection = combineSourcesWithDiff(isReverse, inputLanguageFlow, outputLanguageFlow) {

        val isReverse = isReverse.get()
        val inputLanguage = inputLanguageFlow.filterNotNull().get()
        val outputLanguage = outputLanguageFlow.get()

        val languageCode = if (isReverse) {
            outputLanguage.id
        } else {
            inputLanguage.id
        }

        val isRtl = Locale(languageCode).layoutDirection == View.LAYOUT_DIRECTION_RTL

        val textDirection = if (isRtl) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR

        emit(textDirection)
    }

    fun updateReverse(it: Boolean) {
        isReverse.tryEmit(it)
    }
}