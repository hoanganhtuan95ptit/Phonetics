package com.simple.phonetics.ui.ipa.list

import android.text.style.ForegroundColorSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.base.adapters.IpaViewItem
import com.simple.phonetics.utils.exts.BackgroundColor
import com.simple.state.ResultState

class IpaListViewModel(
    private val getIpaStateAsyncUseCase: GetIpaStateAsyncUseCase
) : BaseViewModel() {

    val title: LiveData<String> = combineSources(translate) {

        val translate = translate.value ?: return@combineSources

        postDifferentValueIfActive(translate["ipa_list_screen_title"])
    }

    @VisibleForTesting
    val ipaState: LiveData<ResultState<List<Ipa>>> = mediatorLiveData {

        getIpaStateAsyncUseCase.execute(param = GetIpaStateAsyncUseCase.Param(sync = false)).collect {

            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(size, theme, translate, ipaState) {

        val size = size.value ?: return@combineSources
        val theme = theme.value ?: return@combineSources

        val state = ipaState.value ?: return@combineSources

        if (state !is ResultState.Success) {

            return@combineSources
        }

        val viewItemList = arrayListOf<ViewItem>()

        val ipaList = state.data

        ipaList.map {

            IpaViewItem(
                id = it.ipa,

                data = it,

                ipa = it.ipa.with(ForegroundColorSpan(theme.colorOnSurface)),
                text = it.examples.firstOrNull().orEmpty().with(ForegroundColorSpan(theme.colorOnSurface)),

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

        postDifferentValueIfActive(viewItemList)
    }
}