package com.simple.phonetics.ui.ipa.list

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.dao.entities.Ipa
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.ui.base.adapters.IpaViewItem
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.BackgroundColor
import com.simple.state.ResultState
import com.unknown.size.uitls.exts.width
import com.unknown.theme.utils.exts.colorOnSurface

class IpaListViewModel(
    private val getIpaStateAsyncUseCase: GetIpaStateAsyncUseCase
) : BaseViewModel() {

    val title: LiveData<String> = combineSourcesWithDiff(translate) {

        val translate = translate.value ?: return@combineSourcesWithDiff

        postValueIfActive(translate["ipa_list_screen_title"])
    }

    @VisibleForTesting
    val ipaState: LiveData<ResultState<List<Ipa>>> = mediatorLiveData {

        getIpaStateAsyncUseCase.execute(param = GetIpaStateAsyncUseCase.Param(sync = false)).collect {

            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, theme, translate, ipaState) {

        val size = size.value ?: return@combineSourcesWithDiff
        val theme = theme.value ?: return@combineSourcesWithDiff

        val state = ipaState.value ?: return@combineSourcesWithDiff

        if (state !is ResultState.Success) {

            return@combineSourcesWithDiff
        }

        val viewItemList = arrayListOf<ViewItem>()

        state.data.map {

            IpaViewItem(
                id = it.ipa,

                data = it,

                ipa = it.ipa
                    .with(ForegroundColor(theme.colorOnSurface)),
                text = it.examples.firstOrNull().orEmpty()
                    .with(ForegroundColor(theme.colorOnSurface)),

                size = Size(
                    width = (size.width - 2 * DP.DP_12) / 3 - 2 * DP.DP_4,
                    height = DP.DP_90
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

        postValueIfActive(viewItemList)
    }
}