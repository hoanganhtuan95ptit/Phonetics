package com.simple.phonetics.ui.home.view.ipa

import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.dao.entities.Ipa
import com.simple.phonetics.Id
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.ui.base.adapters.IpaViewItem
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.BackgroundColor
import com.simple.phonetics.utils.exts.TitleViewItem
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.state.ResultState
import com.unknown.size.uitls.exts.getOrZero

class IpaHomeViewModel(
    private val getIpaStateAsyncUseCase: GetIpaStateAsyncUseCase,
) : BaseViewModel() {

    @VisibleForTesting
    val ipaState: LiveData<ResultState<List<Ipa>>> = mediatorLiveData {

        postValue(ResultState.Start)

        getIpaStateAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val ipaViewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, theme, translate, ipaState) {

        val size = size.value ?: return@combineSourcesWithDiff
        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val state = ipaState.value ?: return@combineSourcesWithDiff

        if (!translate.containsKey("title_ipa") || state !is ResultState.Success) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }

        val viewItemList = arrayListOf<ViewItem>()

        val ipaList = state.data.runCatching { subList(0, 4) }.getOrNull().orEmpty()

        if (ipaList.isNotEmpty()) TitleViewItem(
            id = "TITLE_IPA",
            text = translate["title_ipa"].orEmpty()
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
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

                ipa = it.ipa
                    .with(ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
                text = it.examples.firstOrNull().orEmpty()
                    .with(ForegroundColor(theme.getOrTransparent("colorOnSurface"))),

                size = Size(
                    width = (size.getOrZero("width") - 2 * DP.DP_12) / 3 - 2 * DP.DP_4,
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
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorPrimary"))),
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
                strokeColor = theme.getOrTransparent("colorPrimary"),
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            ),

            imageLeft = null,
            imageRight = null
        ).let {

            viewItemList.add(it)
            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_IPA_2", height = DP.DP_16))
        }

        if (viewItemList.isNotEmpty()) {
            logAnalytics("ipa_home_show")
        }

        postValueIfActive(viewItemList)
    }
}