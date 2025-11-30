package com.simple.phonetics.ui.home.services.ipa

import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.ipa.entities.Ipa
import com.simple.phonetics.Id
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.ui.common.adapters.IpaViewItem
import com.simple.phonetics.ui.base.adapters.SizeViewItem
import com.simple.phonetics.ui.base.adapters.TextSimpleViewItem
import com.simple.phonetics.ui.base.adapters.measureTextViewHeight
import com.simple.phonetics.ui.base.adapters.measureTextViewWidth
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.BackgroundColor
import com.simple.state.ResultState
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary

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

    val ipaViewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, style, theme, translate, ipaState) {

        val size = size.value ?: return@combineSourcesWithDiff
        val style = style.value ?: return@combineSourcesWithDiff
        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val state = ipaState.value ?: return@combineSourcesWithDiff

        if (!translate.containsKey("title_ipa") || state !is ResultState.Success) {

            postValue(emptyList())
            return@combineSourcesWithDiff
        }


        val viewItemList = arrayListOf<ViewItem>()


        val ipaList = state.data.runCatching { subList(0, 4) }.getOrNull().orEmpty()

        if (ipaList.isNotEmpty()) TextSimpleViewItem(
            id = "TITLE_IPA",
            text = translate["title_ipa"].orEmpty()
                .with(Bold, ForegroundColor(theme.colorOnSurface)),
            textStyle = R.style.TextAppearance_MaterialComponents_Headline6,
            margin = Margin(
                marginHorizontal = DP.DP_4
            )
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
                    .with(ForegroundColor(theme.colorOnSurface)),
                text = it.examples.firstOrNull().orEmpty()
                    .with(ForegroundColor(theme.colorOnSurface)),

                size = Size(
                    width = (size.width - 6 * DP.DP_4 - 2 * DP.DP_12) / 3,
                ),
                margin = Margin(
                    margin = DP.DP_4
                ),
                background = Background(
                    cornerRadius = DP.DP_16,
                    backgroundColor = it.BackgroundColor(theme = theme)
                )
            )
        }.map {

            it.measure(appSize = size, style = style)
            it
        }.let {

            viewItemList.addAll(it)
        }


        val actionText = translate["action_view_all_ipa"].orEmpty()
            .with(Bold, ForegroundColor(theme.colorPrimary))

        val ipaHeight = viewItemList.filterIsInstance<IpaViewItem>().firstOrNull()?.size?.height ?: DP.DP_72

        val textWidth = measureTextViewWidth(actionText.textChar, size.width, style["TextAppearance_MaterialComponents_Body1"] ?: return@combineSourcesWithDiff)
        val textHeight = measureTextViewHeight(actionText.textChar, size.width, style["TextAppearance_MaterialComponents_Body1"] ?: return@combineSourcesWithDiff)

        if (ipaList.isNotEmpty()) TextSimpleViewItem(
            id = Id.IPA_LIST,
            text = actionText,
            textStyle = R.style.TextAppearance_MaterialComponents_Body1,
            textSize = Size(
                height = DP.DP_76,
                width = ViewGroup.LayoutParams.WRAP_CONTENT
            ),

            size = Size(
                width = textWidth + 2 * DP.DP_16,
                height = ipaHeight
            ),
            margin = Margin(
                marginVertical = DP.DP_4,
                marginHorizontal = DP.DP_4
            ),
            padding = Padding(
                paddingVertical = (ipaHeight - textHeight) / 2,
                paddingHorizontal = DP.DP_16
            ),
            background = Background(
                strokeColor = theme.colorPrimary,
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            ),
        ).let {

            viewItemList.add(it)
            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_IPA_2", height = DP.DP_16))
        }

        if (viewItemList.isNotEmpty()) {
            logAnalytics("ipa_home_show")
        }

        viewItemList.forEach {

            if (it is SizeViewItem) it.measure(size, style)
        }

        postValueIfActive(viewItemList)
    }
}