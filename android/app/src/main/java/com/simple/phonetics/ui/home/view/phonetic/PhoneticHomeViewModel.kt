package com.simple.phonetics.ui.home.view.phonetic

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.simple.adapter.SpaceViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.domain.usecase.phonetics.SyncPhoneticAsyncUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.state.ResultState

class PhoneticHomeViewModel(
    private val syncPhoneticAsyncUseCase: SyncPhoneticAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val phoneticState: LiveData<ResultState<Map<String, SyncPhoneticAsyncUseCase.State>>> = mediatorLiveData {

//        syncPhoneticAsyncUseCase.execute().collect {

//            postValue(it)
//        }
    }

    val pairViewList: LiveData<List<Pair<String, CharSequence>>> = combineSources(theme, translate, phoneticState) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val state = phoneticState.value ?: return@combineSources

        if (state !is ResultState.Running) {

            postDifferentValue(emptyList())
            return@combineSources
        }

        state.data.values.filterIsInstance<SyncPhoneticAsyncUseCase.State.SyncPhonetics>().map {

            val key = it.code.lowercase()

            val ipaName = if (translate.containsKey(key)) {
                translate[key] ?: it.name
            } else {
                it.name
            }

            val percentWrap = (it.percent * 100).toInt()

            val text = if (percentWrap < 100) {
                translate["message_start_sync_phonetics"].orEmpty()
                    .replace("\$ipa_name", ipaName)
                    .replace("\$percent", "$percentWrap")
                    .with(ForegroundColorSpan(theme.colorOnSurface))
                    .with(ipaName, StyleSpan(Typeface.BOLD))
                    .with("${percentWrap}%", StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary))
            } else {
                translate["message_completed_sync_phonetics"].orEmpty()
                    .replace("\$ipa_name", ipaName)
                    .with(ForegroundColorSpan(theme.colorOnSurface))
                    .with(ipaName, StyleSpan(Typeface.BOLD))
            }

            key to text
        }.let {

            postDifferentValueIfActive(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(pairViewList) {

        val viewItemList = arrayListOf<ViewItem>()

        pairViewList.getOrEmpty().map {

            NoneTextViewItem(
                id = "phonetic_" + it.first,
                text = "",
                textStyle = TextStyle(
                    textSize = 14f
                ),
                textPadding = Padding(
                    paddingVertical = DP.DP_8,
                    paddingHorizontal = DP.DP_4
                )
            )
        }.let {

            viewItemList.add(SpaceViewItem(id = "SPACE_TITLE_AND_PHONETIC_0", height = DP.DP_16))
            viewItemList.addAll(it)
        }

        postDifferentValueIfActive(viewItemList)
    }
}