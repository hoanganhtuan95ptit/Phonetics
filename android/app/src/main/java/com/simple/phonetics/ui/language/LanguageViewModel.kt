package com.simple.phonetics.ui.language

import android.graphics.Color
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.domain.usecase.key_translate.GetKeyTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageSupportUseCase
import com.simple.phonetics.domain.usecase.language.UpdateLanguageInputUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.language.adapters.LanguageLoadingViewItem
import com.simple.phonetics.ui.language.adapters.LanguageStateViewItem
import com.simple.phonetics.ui.language.adapters.LanguageViewItem
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.appTheme
import com.simple.phonetics.utils.exts.with
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.isSuccess
import com.simple.state.toRunning
import com.simple.state.toSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LanguageViewModel(
    private val getLanguageSupportUseCase: GetLanguageSupportUseCase,
    private val updateLanguageInputUseCase: UpdateLanguageInputUseCase,
    private val getKeyTranslateAsyncUseCase: GetKeyTranslateAsyncUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase
) : com.simple.coreapp.ui.base.fragments.transition.TransitionViewModel() {

    val theme: LiveData<AppTheme> = mediatorLiveData {

        appTheme.collect {

            postDifferentValue(it)
        }
    }

    @VisibleForTesting
    val keyTranslateMap: LiveData<Map<String, String>> = mediatorLiveData {

        getKeyTranslateAsyncUseCase.execute().collect {

            postDifferentValue(it)
        }
    }

    val headerInfo: LiveData<HeaderInfo> = combineSources(theme, keyTranslateMap) {

        val theme = theme.value ?: return@combineSources
        val keyTranslateMap = keyTranslateMap.value ?: return@combineSources

        val info = HeaderInfo(
            title = keyTranslateMap["title_language"].orEmpty()
                .with(ForegroundColorSpan(theme.colorOnBackground)),
            message = keyTranslateMap["message_select_language"].orEmpty()
                .with(ForegroundColorSpan(theme.colorOnBackgroundVariant)),
        )

        postDifferentValue(info)
    }

    @VisibleForTesting
    val languageOld: LiveData<Language> = mediatorLiveData {

        getLanguageInputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val languageSelected: LiveData<Language> = combineSources(languageOld) {

        languageOld.value?.let {

            postDifferentValue(it)
        }
    }

    @VisibleForTesting
    val languageListState: LiveData<ResultState<List<Language>>> = mediatorLiveData {

        postValue(ResultState.Start)

        getLanguageSupportUseCase.execute().collect {

            postValue(it)
        }
    }

    val changeLanguageState: LiveData<ResultState<Map<String, UpdateLanguageInputUseCase.State>>> = MediatorLiveData()

    val languageViewItemList: LiveData<List<ViewItem>> = listenerSources(theme, keyTranslateMap, languageSelected, languageListState, changeLanguageState) {

        val theme = theme.value ?: return@listenerSources
        val state = languageListState.get()

        if (state is ResultState.Start) {

            postValue(theme.toListLoadingViewItem())
            return@listenerSources
        }

        if (state !is ResultState.Success) {

            return@listenerSources
        }

        val languageSelected = languageSelected.value


        val listLanguage = state.data.map {

            val isSelected = it.id == languageSelected?.id

            LanguageViewItem(
                data = it,
                name = it.name.with(ForegroundColorSpan(if (isSelected) theme.colorOnPrimaryVariant else theme.colorOnSurface)),
                image = it.image,
                isSelected = isSelected,
                background = com.simple.coreapp.ui.view.round.Background(
                    strokeColor = if (isSelected) theme.colorPrimary else theme.colorDivider,
                    strokeDashGap = DP.DP_4,
                    strokeDashWidth = DP.DP_4,
                    backgroundColor = if (isSelected) theme.colorPrimaryVariant else Color.TRANSPARENT
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

        postDifferentValue(viewItemList)
    }

    val buttonInfo: LiveData<ButtonInfo> = listenerSources(theme, languageOld, languageSelected, changeLanguageState, keyTranslateMap) {

        val theme = theme.value ?: return@listenerSources
        val keyTranslateMap = keyTranslateMap.value ?: return@listenerSources

        val languageOld = languageOld.value
        val languageSelected = languageSelected.value

        val changeLanguageState = changeLanguageState.value

        val isSelected = languageOld?.id != languageSelected?.id

        val isClickable = isSelected && !changeLanguageState.isSuccess()

        val info = ButtonInfo(
            text = keyTranslateMap["action_confirm_change_language"]
                .orEmpty()
                .with(ForegroundColorSpan(if (isSelected) theme.colorOnPrimary else theme.colorOnSurface)),
            isClickable = isClickable,
            isShowLoading = changeLanguageState != null && !changeLanguageState.isCompleted(),
            background = com.simple.coreapp.ui.view.round.Background(
                strokeColor = if (isSelected) theme.colorPrimary else theme.colorDivider,
                backgroundColor = if (isSelected) theme.colorPrimary else Color.TRANSPARENT
            )
        )

        postDifferentValue(info)
    }

    fun updateLanguageSelected(data: Language) {

        val changeLanguageState = changeLanguageState.value

        if (changeLanguageState != null && !changeLanguageState.isCompleted()) {

            return
        }

        languageSelected.postDifferentValue(data)
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

    private fun AppTheme.toListLoadingViewItem() = arrayListOf<ViewItem>().apply {

        add(toLoadingViewItem())

        add(toLoadingViewItem())

        add(toLoadingViewItem())
    }

    private fun AppTheme.toLoadingViewItem() = LanguageLoadingViewItem(
        loadingColor = colorLoading,
        background = com.simple.coreapp.ui.view.round.Background(
            strokeColor = colorDivider,
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
        val keyTranslateMap = keyTranslateMap.value ?: return@apply

        if (this@toViewItem is UpdateLanguageInputUseCase.State.START) LanguageStateViewItem(
            data = UpdateLanguageInputUseCase.State.START.javaClass.simpleName,
            name = keyTranslateMap["message_start_sync"].orEmpty()
        ).let {

            add(it)
        }

        (if (this@toViewItem is UpdateLanguageInputUseCase.State.SYNC_PHONETICS && this@toViewItem.percent < 1) LanguageStateViewItem(
            data = this@toViewItem.name,
            name = keyTranslateMap["message_start_sync_phonetics"].orEmpty()
                .replace("\$ipa_name", this@toViewItem.name)
                .replace("\$percent", (this@toViewItem.percent * 100).toInt().toString())
                .with(this@toViewItem.name, StyleSpan(Typeface.BOLD))
                .with("${(this@toViewItem.percent * 100).toInt()}%", StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary))
        ) else if (this@toViewItem is UpdateLanguageInputUseCase.State.SYNC_PHONETICS) LanguageStateViewItem(
            data = this@toViewItem.name,
            name = keyTranslateMap["message_completed_sync_phonetics"].orEmpty()
                .replace("\$ipa_name", this@toViewItem.name)
                .with(this@toViewItem.name, StyleSpan(Typeface.BOLD))
        ) else {
            null
        })?.let {

            add(it)
        }

        (if (this@toViewItem is UpdateLanguageInputUseCase.State.SYNC_TRANSLATE && this@toViewItem.percent < 1) LanguageStateViewItem(
            data = this@toViewItem.name,
            name = keyTranslateMap["message_start_sync_translate"].orEmpty()
                .replace("\$percent", (this@toViewItem.percent * 100).toInt().toString())
                .with("${(this@toViewItem.percent * 100).toInt()}%", StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary))
        ) else if (this@toViewItem is UpdateLanguageInputUseCase.State.SYNC_TRANSLATE) LanguageStateViewItem(
            data = this@toViewItem.name,
            name = keyTranslateMap["message_completed_sync_translate"].orEmpty()
        ) else {
            null
        })?.let {

            add(it)
        }

        if (this@toViewItem is UpdateLanguageInputUseCase.State.COMPLETED) LanguageStateViewItem(
            data = UpdateLanguageInputUseCase.State.COMPLETED.javaClass.simpleName,
            name = keyTranslateMap["message_sync_completed"].orEmpty()
        ).let {

            add(it)
        }
    }

    data class HeaderInfo(
        val title: CharSequence,
        val message: CharSequence,
    )

    data class ButtonInfo(
        val text: CharSequence,

        val isClickable: Boolean,
        val isShowLoading: Boolean,

        val background: com.simple.coreapp.ui.view.round.Background,
    )
}