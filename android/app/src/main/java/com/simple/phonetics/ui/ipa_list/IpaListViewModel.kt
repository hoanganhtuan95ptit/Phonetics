package com.simple.phonetics.ui.ipa_list

import android.text.style.ForegroundColorSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.round.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.domain.usecase.ipa.GetIpaStateAsyncUseCase
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.ui.base.CommonViewModel
import com.simple.phonetics.ui.base.adapters.IpaViewItem
import com.simple.phonetics.utils.exts.BackgroundColor
import com.simple.state.ResultState

class IpaListViewModel(
    private val getIpaStateAsyncUseCase: GetIpaStateAsyncUseCase
) : CommonViewModel() {

    val title: LiveData<String> = combineSources(translate) {

        val translate = translate.value ?: return@combineSources

        postDifferentValueIfActive(translate["ipa_list_screen_title"])
    }

    @VisibleForTesting
    val ipaState: LiveData<ResultState<List<Ipa>>> = mediatorLiveData {

        getIpaStateAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(theme, translate, ipaState) {

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

                ipa = it.ipa,
                text = it.examples.firstOrNull().orEmpty().with(ForegroundColorSpan(theme.colorOnSurface)),

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