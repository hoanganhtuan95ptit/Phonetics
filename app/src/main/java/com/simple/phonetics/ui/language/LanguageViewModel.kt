package com.simple.phonetics.ui.language

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.LoadingViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.key_translate.GetKeyTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageSupportUseCase
import com.simple.phonetics.domain.usecase.language.UpdateLanguageInputUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.base.TransitionViewModel
import com.simple.phonetics.ui.language.adapters.LanguageViewItem
import com.simple.state.ResultState
import com.simple.state.isCompleted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LanguageViewModel(
    private val getLanguageSupportUseCase: GetLanguageSupportUseCase,
    private val updateLanguageInputUseCase: UpdateLanguageInputUseCase,
    private val getKeyTranslateAsyncUseCase: GetKeyTranslateAsyncUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase
) : TransitionViewModel() {

    private val itemLoading = listOf(
        LoadingViewItem(R.layout.item_language_loading),
        LoadingViewItem(R.layout.item_language_loading),
        LoadingViewItem(R.layout.item_language_loading),
    )

    @VisibleForTesting
    val keyTranslateMap: LiveData<Map<String, String>> = mediatorLiveData {

        getKeyTranslateAsyncUseCase.execute().collect {

            postDifferentValue(it)
        }
    }

    val message: LiveData<String> = combineSources(keyTranslateMap) {

        postDifferentValue(keyTranslateMap.getOrEmpty()["message_select_language"])
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

    @VisibleForTesting
    val languageViewItemList: LiveData<List<ViewItem>> = listenerSources(languageSelected, languageListState) {

        val state = languageListState.get()

        if (state is ResultState.Start) {

            postValue(itemLoading)
            return@listenerSources
        }

        if (state !is ResultState.Success) {

            return@listenerSources
        }

        val languageSelected = languageSelected.value

        state.data.map {

            LanguageViewItem(
                data = it,
                name = it.name,
                image = it.image,
                isSelected = it.id == languageSelected?.id
            )
        }.let {

            postValue(it)
        }
    }

    val languageViewItemListEvent = languageViewItemList.toEvent()

    val changeLanguageState: LiveData<ResultState<UpdateLanguageInputUseCase.State>> = MediatorLiveData()


    val buttonInfo: LiveData<ButtonInfo> = listenerSources(languageOld, languageSelected, changeLanguageState, keyTranslateMap) {

        val keyTranslateMap = keyTranslateMap.get()

        val languageOld = languageOld.value
        val languageSelected = languageSelected.value

        val changeLanguageState = changeLanguageState.value

        val isSelected = languageOld?.id != languageSelected?.id

        val info = ButtonInfo(
            text = keyTranslateMap["action_confirm_change_language"].orEmpty(),
            isSelected = isSelected,
            isClickable = isSelected,
            isShowLoading = changeLanguageState != null && !changeLanguageState.isCompleted()
        )

        postDifferentValue(info)
    }


    fun updateLanguageSelected(data: Language) {

        languageSelected.postDifferentValue(data)
    }

    fun changeLanguageInput() = viewModelScope.launch(handler + Dispatchers.IO) {

        val languageSelected = languageSelected.value ?: return@launch

        val param = UpdateLanguageInputUseCase.Param(
            language = languageSelected
        )

        updateLanguageInputUseCase.execute(param).collect {

            changeLanguageState.postDifferentValue(it)
        }
    }

    data class ButtonInfo(
        val text: String,
        val isSelected: Boolean,
        val isClickable: Boolean,
        val isShowLoading: Boolean,
    )
}