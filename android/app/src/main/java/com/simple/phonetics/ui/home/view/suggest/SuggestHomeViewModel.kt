package com.simple.phonetics.ui.home.view.suggest

import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.Id
import com.simple.phonetics.domain.usecase.phonetics.suggest.GetPhoneticsSuggestUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel

class SuggestHomeViewModel(
    private val getPhoneticsSuggestUseCase: GetPhoneticsSuggestUseCase
) : BaseViewModel() {

    val text: LiveData<String> = MediatorLiveData()

    val keyboardShow: LiveData<Boolean> = MediatorLiveData()

    val viewItemList: LiveData<List<ViewItem>> = combineSources(theme, text, inputLanguage, keyboardShow) {

        val text = text.get()
        val theme = theme.get()

        if (text.isEmpty() || !keyboardShow.get()) {

            postDifferentValue(emptyList())
            return@combineSources
        }

        val list = getPhoneticsSuggestUseCase.execute(GetPhoneticsSuggestUseCase.Param(text))

        list.sortedBy {

            it.text.length
        }.map {

            ClickTextViewItem(
                id = Id.SUGGEST + "-" + it.text.lowercase(),
                data = it,
                text = it.text
                    .with(ForegroundColorSpan(theme.colorOnSurface)),
                textSize = Size(
                    width = ViewGroup.LayoutParams.WRAP_CONTENT,
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                ),
                textPadding = Padding(
                    paddingVertical = DP.DP_8,
                    paddingHorizontal = DP.DP_16
                ),
                textStyle = TextStyle(
                    textGravity = Gravity.CENTER
                ),
                margin = Margin(
                    marginHorizontal = DP.DP_8
                ),
                background = Background(
                    strokeWidth = DP.DP_1,
                    cornerRadius = DP.DP_100,
                    strokeColor = theme.colorOnBackgroundVariant
                )
            )
        }.let {

            postDifferentValue(it)
        }
    }

    fun setText(selectedWord: String) {

        text.postDifferentValue(selectedWord)
    }

    fun setKeyboardShow(open: Boolean) {

        keyboardShow.postDifferentValue(open)
    }
}