package com.simple.phonetics.ui.phonetics.view.ipa

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.SpaceViewItem
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
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.Id
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.ui.base.CommonViewModel
import com.simple.phonetics.ui.base.adapters.IpaViewItem
import com.simple.phonetics.utils.exts.BackgroundColor
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.state.ResultState

class IpaViewModel(
    private val getIpaStateAsyncUseCase: GetIpaStateAsyncUseCase
) : CommonViewModel() {

    @VisibleForTesting
    val ipaState: LiveData<ResultState<List<Ipa>>> = mediatorLiveData {

        postValue(ResultState.Start)

        getIpaStateAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val ipaViewItemList: LiveData<List<ViewItem>> = combineSources(size, theme, translate, ipaState) {

        val size = size.value ?: return@combineSources
        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val state = ipaState.value ?: return@combineSources

        if (state !is ResultState.Success) {

            postDifferentValue(emptyList())
            return@combineSources
        }

        val viewItemList = arrayListOf<ViewItem>()

        val ipaList = state.data.runCatching { subList(0, 4) }.getOrNull().orEmpty()

        if (ipaList.isNotEmpty()) TitleViewItem(
            id = "TITLE_IPA",
            text = translate["title_ipa"].orEmpty()
                .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnSurface)),
            textSize = 20f,
        ).let {

            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_IPA_0", height = DP.DP_16))
            viewItemList.add(it)
            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_IPA_1", height = DP.DP_8))
        }

        ipaList.map {

            IpaViewItem(
                id = it.ipa,

                data = it,

                ipa = it.ipa.with(ForegroundColorSpan(theme.colorOnSurface)),
                text = it.examples.firstOrNull().orEmpty().with(ForegroundColorSpan(theme.colorOnSurface)),

                size = Size(
                    width = (size.width - 2 * DP.DP_12) / 3 - 2 * DP.DP_4,
                    height = DP.DP_76
                ),
                margin = Margin(
                    margin = DP.DP_4
                ),
                background = Background(
                    cornerRadius = DP.DP_16,
                    backgroundColor = it.BackgroundColor(theme = theme)
                )
            )
        }.let {

            viewItemList.addAll(it)
        }

        if (ipaList.isNotEmpty()) ClickTextViewItem(
            id = Id.IPA_LIST,
            text = translate["action_view_all_ipa"].orEmpty()
                .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary)),
            textSize = Size(
                height = DP.DP_76,
                width = ViewGroup.LayoutParams.WRAP_CONTENT
            ),
            textStyle = TextStyle(
                textGravity = Gravity.CENTER
            ),
            textPadding = Padding(
                left = DP.DP_16,
                right = DP.DP_16
            ),

            margin = Margin(
                marginVertical = DP.DP_4,
                marginHorizontal = DP.DP_4
            ),
            background = Background(
                strokeColor = theme.colorPrimary,
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            ),

            imageLeft = null,
            imageRight = null
        ).let {

            viewItemList.add(it)
            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_IPA_2", height = DP.DP_16))
        }

        postDifferentValueIfActive(viewItemList)
    }
}