package com.simple.phonetics.ui.home.services.ipa

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.utils.ext.DP
import com.simple.ipa.entities.Ipa
import com.simple.phonetics.Id
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.ui.base.adapters.GridViewItem
import com.simple.phonetics.ui.base.adapters.TextSimpleViewItem
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.common.adapters.IpaViewItem
import com.simple.phonetics.ui.common.adapters.PrecomputeViewItem
import com.simple.phonetics.utils.combineState
import com.simple.phonetics.utils.exts.BackgroundColor
import com.simple.phonetics.utils.exts.dp
import com.simple.phonetics.utils.exts.withStyleBodyLarge
import com.simple.phonetics.utils.exts.withStyleBodySmall
import com.simple.phonetics.utils.exts.withStyleTitleLarge
import com.simple.phonetics.utils.mutableStateFlow
import com.simple.state.ResultState
import com.simple.ui.precompute.text.build
import com.simple.ui.precompute.text.span.BigBold
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.with
import com.unknown.coroutines.launchCollect
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class IpaHomeViewModel(
    private val getIpaStateAsyncUseCase: GetIpaStateAsyncUseCase,
) : BaseViewModel() {

    @VisibleForTesting
    val ipaState: StateFlow<ResultState<List<Ipa>>> = mutableStateFlow(
        initialValue = ResultState.Idle as ResultState<List<Ipa>>
    ) {
        getIpaStateAsyncUseCase.execute().collect {
            value = it
        }
    }

    val viewItemList: StateFlow<List<ViewItem>> = combineState(
        sizes,
        themes,
        strings,
        ipaState,
        initialValue = emptyList()
    ) { sizes, themes, strings, state ->

        if (!strings.containsKey("title_ipa") || state !is ResultState.Success) {
            value = emptyList()
            return@combineState
        }


        val childViewItemList = arrayListOf<PrecomputeViewItem>()

        val ipaList = state.data.take(4)

        ipaList.map {

            IpaViewItem(
                id = it.ipa,
                maxWidth = (sizes.width - 2 * 12.dp().toInt()) / 3,

                data = it,

                ipa = it.ipa
                    .withStyleTitleLarge()
                    .with(BigBold, BigForegroundColor(themes.colorOnSurface))
                    .build(),
                text = it.examples.firstOrNull().orEmpty()
                    .withStyleBodySmall()
                    .with(BigForegroundColor(themes.colorOnSurface))
                    .build(),

                background = Background(
                    cornerRadius = DP.DP_16,
                    backgroundColor = it.BackgroundColor(theme = themes)
                )
            )
        }.let {

            childViewItemList.addAll(it)
        }

        if (childViewItemList.isNotEmpty()) TextSimpleViewItem(
            id = Id.IPA_LIST,
            maxWidth = sizes.width,

            text = strings["action_view_all_ipa"].orEmpty()
                .withStyleBodyLarge()
                .with(BigBold, BigForegroundColor(themes.colorPrimary))
                .build(),

            textSize = Size(
                height = 90.dp().toInt()
            ),

            textPadding = Padding(
                paddingHorizontal = 16.dp().toInt(),
            ),

            padding = Padding(
                top = 4.dp().toInt(),
                left = 4.dp().toInt(),
            ),

            background = Background(
                strokeColor = themes.colorPrimary,
                strokeWidth = DP.DP_2,
                cornerRadius = DP.DP_16
            ),
        ).let {

            childViewItemList.add(it)
        }


        val viewItemList = arrayListOf<ViewItem>()

        if (ipaList.isNotEmpty()) TextSimpleViewItem(
            id = "TITLE_IPA",
            maxWidth = sizes.width,
            text = strings["title_ipa"].orEmpty()
                .withStyleTitleLarge()
                .with(BigBold, BigForegroundColor(themes.colorOnSurface))
                .build(),
            textPadding = Padding(
                top = 16.dp().toInt(),
                left = 4.dp().toInt(),
                right = 4.dp().toInt(),
                bottom = 8.dp().toInt()
            )
        ).let {

            viewItemList.add(it)
        }

        if (childViewItemList.isNotEmpty()) GridViewItem(
            id = "TITLE_IPA",
            maxWidth = sizes.width - 2 * 12.dp().toInt(),
            column = 3,
            viewItems = childViewItemList
        ).let {

            viewItemList.add(it)
        }

        value = viewItemList
    }

    init {

        viewItemList.map { it.isNotEmpty() }.distinctUntilChanged().launchCollect(viewModelScope) {

            logAnalytics("feature_ipa_home_show_$it")
        }
    }
}