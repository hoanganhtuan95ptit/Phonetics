package com.simple.phonetics.ui.language

import android.graphics.Color
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.toRich
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.listenerSourcesWithDiff
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.domain.usecase.language.GetLanguageSupportAsyncUseCase
import com.simple.phonetics.domain.usecase.language.input.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.input.UpdateLanguageInputUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.ui.language.adapters.LanguageLoadingViewItem
import com.simple.phonetics.ui.language.adapters.LanguageStateViewItem
import com.simple.phonetics.ui.language.adapters.LanguageViewItem
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.isSuccess
import com.simple.state.toRunning
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LanguageViewModel(
    private val updateLanguageInputUseCase: UpdateLanguageInputUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase,
    private val getLanguageSupportAsyncUseCase: GetLanguageSupportAsyncUseCase
) : BaseViewModel() {

    val headerInfo: LiveData<HeaderInfo> = combineSourcesWithDiff(theme, translate) {

        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val info = HeaderInfo(
            title = translate["title_language"].orEmpty()
                .with(ForegroundColor(theme.getOrTransparent("colorOnBackground"))),
            message = translate["message_select_language"].orEmpty()
                .with(ForegroundColor(theme.getOrTransparent("colorOnBackgroundVariant"))),
        )

        postValue(info)
    }

    @VisibleForTesting
    val languageOld: LiveData<Language> = mediatorLiveData {

        getLanguageInputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val languageSelected: LiveData<Language> = combineSourcesWithDiff(languageOld) {

        languageOld.value?.let {

            postValue(it)
        }
    }

    @VisibleForTesting
    val languageListState: LiveData<ResultState<List<Language>>> = mediatorLiveData {

        postValue(ResultState.Start)

        getLanguageSupportAsyncUseCase.execute(GetLanguageSupportAsyncUseCase.Param(sync = false)).collect {

            postValue(it)
        }
    }

    val changeLanguageState: LiveData<ResultState<Map<String, UpdateLanguageInputUseCase.State>>> = MediatorLiveData()

    val languageViewItemList: LiveData<List<ViewItem>> = listenerSourcesWithDiff(theme, translate, languageSelected, languageListState, changeLanguageState) {

        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff

        val state = languageListState.value ?: return@listenerSourcesWithDiff

        if (state is ResultState.Start) {

            postValue(theme.toListLoadingViewItem())
            return@listenerSourcesWithDiff
        }

        if (state !is ResultState.Success) {

            return@listenerSourcesWithDiff
        }

        val languageSelected = languageSelected.value


        val listLanguage = state.data.map {

            val isSelected = it.id == languageSelected?.id

            val key = "language_" + it.id.lowercase()

            val name = if (translate.containsKey(key)) {
                translate[key] ?: it.name
            } else {
                it.name
            }

            LanguageViewItem(
                data = it,
                name = name
                    .with(ForegroundColor(if (isSelected) theme.getOrTransparent("colorOnPrimaryVariant") else theme.getOrTransparent("colorOnSurface"))),

                image = it.image,

                isSelected = isSelected,

                background = Background(
                    strokeWidth = DP.DP_1,
                    strokeColor = if (isSelected) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorDivider"),
                    strokeDashGap = DP.DP_4,
                    strokeDashWidth = DP.DP_4,
                    backgroundColor = if (isSelected) theme.getOrTransparent("colorPrimaryVariant") else Color.TRANSPARENT
                ),
            )
        }

        val listLanguageState = changeLanguageState.value?.toViewItem() ?: emptyList()


        val viewItemList = arrayListOf<ViewItem>()

        if (listLanguageState.isEmpty()) {

            viewItemList.addAll(listLanguage)
        } else {

            viewItemList.addAll(listLanguage.filter { it.isSelected })
        }

        viewItemList.addAll(listLanguageState)

        postValue(viewItemList)
    }

    val buttonInfo: LiveData<ButtonInfo> = listenerSourcesWithDiff(theme, languageOld, languageSelected, changeLanguageState, translate) {

        val theme = theme.value ?: return@listenerSourcesWithDiff
        val translate = translate.value ?: return@listenerSourcesWithDiff

        val languageOld = languageOld.value
        val languageSelected = languageSelected.value

        val changeLanguageState = changeLanguageState.value

        val isSelected = languageOld?.id != languageSelected?.id

        val isClickable = isSelected && !changeLanguageState.isSuccess()

        val info = ButtonInfo(
            text = translate["action_confirm_change_language"]
                .orEmpty()
                .with(ForegroundColor(if (isSelected) theme.getOrTransparent("colorOnPrimary") else theme.getOrTransparent("colorOnSurface"))),
            isClickable = isClickable,
            isShowLoading = changeLanguageState != null && !changeLanguageState.isCompleted(),
            background = Background(
                strokeWidth = DP.DP_1,
                strokeColor = if (isSelected) theme.getOrTransparent("colorPrimary") else theme.getOrTransparent("colorDivider"),
                backgroundColor = if (isSelected) theme.getOrTransparent("colorPrimary") else Color.TRANSPARENT
            )
        )

        postValue(info)
    }

    fun updateLanguageSelected(data: Language) {

        val changeLanguageState = changeLanguageState.value

        if (changeLanguageState != null && !changeLanguageState.isCompleted()) {

            return
        }

        languageSelected.postValue(data)
    }

    fun changeLanguageInput() = viewModelScope.launch(handler + Dispatchers.IO) {

        val changeLanguageState = changeLanguageState.value

        if (changeLanguageState != null && !changeLanguageState.isCompleted()) {

            return@launch
        }

        // kiểm tra trước khi cập nhật lại ngôn ngữ tìm phiên âm
        val languageSelected = languageSelected.value ?: return@launch

        if (languageSelected.id == languageOld.value?.id) {

            return@launch
        }

        // cập nhật ngôn ngữ phiên âm
        val param = UpdateLanguageInputUseCase.Param(
            language = languageSelected
        )

        updateLanguageInputUseCase.execute(param).collect {

            this@LanguageViewModel.changeLanguageState.postValue(it)
        }
    }

    private fun Map<String, Int>.toListLoadingViewItem() = arrayListOf<ViewItem>().apply {

        add(toLoadingViewItem())

        add(toLoadingViewItem())

        add(toLoadingViewItem())
    }

    private fun Map<String, Int>.toLoadingViewItem() = LanguageLoadingViewItem(
        loadingColor = getOrTransparent("colorLoading"),
        background = Background(
            strokeColor = getOrTransparent("colorDivider"),
            strokeDashGap = DP.DP_4,
            strokeDashWidth = DP.DP_4,
            backgroundColor = Color.TRANSPARENT
        )
    )

    private fun ResultState<Map<String, UpdateLanguageInputUseCase.State>>.toViewItem() = arrayListOf<ViewItem>().apply {

        val list = this@toViewItem.toSuccess()?.data?.values ?: this@toViewItem.toRunning()?.data?.values ?: return@apply

        list.forEach {

            addAll(it.toViewItem())
        }
    }

    private fun UpdateLanguageInputUseCase.State.toViewItem() = arrayListOf<ViewItem>().apply {

        val theme = theme.value ?: return@apply
        val translate = translate.value ?: return@apply

        if (this@toViewItem is UpdateLanguageInputUseCase.State.Start) LanguageStateViewItem(
            data = UpdateLanguageInputUseCase.State.Start.javaClass.simpleName,
            name = translate["message_start_sync"].orEmpty().toRich()
        ).let {

            add(it)
        }

        if (this@toViewItem is UpdateLanguageInputUseCase.State.SyncPhonetics) this@toViewItem.toViewItem(theme = theme, translate = translate).let {

            addAll(it)
        }

        if (this@toViewItem is UpdateLanguageInputUseCase.State.SyncTranslate) this@toViewItem.toViewItem(theme = theme, translate = translate).let {

            addAll(it)
        }

        if (this@toViewItem is UpdateLanguageInputUseCase.State.Completed) LanguageStateViewItem(
            data = UpdateLanguageInputUseCase.State.Completed.javaClass.simpleName,
            name = translate["message_sync_completed"].orEmpty().toRich()
        ).let {

            add(it)
        }
    }

    private fun UpdateLanguageInputUseCase.State.SyncPhonetics.toViewItem(theme: Map<String, Int>, translate: Map<String, String>) = arrayListOf<ViewItem>().apply {

        val key = "ipa_" + code.lowercase()

        val ipaName = if (translate.containsKey(key)) {
            translate[key] ?: name
        } else {
            name
        }

        val percentWrap = (percent * 100).toInt()

        val viewItem = if (percentWrap < 100) LanguageStateViewItem(
            data = ipaName,
            name = translate["message_start_sync_phonetics"].orEmpty()
                .replace("\$ipa_name", ipaName)
                .replace("\$percent", "$percentWrap")
                .with(ipaName, Bold)
                .with("${percentWrap}%", Bold, ForegroundColor(theme.getOrTransparent("colorPrimary")))
        ) else LanguageStateViewItem(
            data = ipaName,
            name = translate["message_completed_sync_phonetics"].orEmpty()
                .replace("\$ipa_name", ipaName)
                .with(ipaName, Bold)
        )

        add(viewItem)
    }

    private fun UpdateLanguageInputUseCase.State.SyncTranslate.toViewItem(theme: Map<String, Int>, translate: Map<String, String>) = arrayListOf<ViewItem>().apply {

        val percentWrap = (percent * 100).toInt()

        val viewItem = if (percentWrap < 100) LanguageStateViewItem(
            data = "SYNC_TRANSLATE",
            name = translate["message_start_sync_translate"].orEmpty()
                .replace("\$percent", percentWrap.toString())
                .with("${percentWrap}%", Bold, ForegroundColor(theme.getOrTransparent("colorPrimary")))
        ) else LanguageStateViewItem(
            data = "SYNC_TRANSLATE",
            name = translate["message_completed_sync_translate"].orEmpty().toRich()
        )

        add(viewItem)
    }

    data class HeaderInfo(
        val title: RichText,
        val message: RichText,
    )

    data class ButtonInfo(
        val text: RichText,

        val isClickable: Boolean,
        val isShowLoading: Boolean,

        val background: Background,
    )
}