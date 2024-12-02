package com.simple.phonetics.ui.language

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.simple.adapter.LoadingViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.key_translate.GetKeyTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageSupportUseCase
import com.simple.phonetics.domain.usecase.language.UpdateLanguageInputUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.base.TransitionViewModel
import com.simple.phonetics.ui.language.adapters.LanguageStateViewItem
import com.simple.phonetics.ui.language.adapters.LanguageViewItem
import com.simple.state.ResultState
import com.simple.state.doFailed
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

    val title: LiveData<String> = combineSources(keyTranslateMap) {

        postDifferentValue(keyTranslateMap.getOrEmpty()["title_language"])
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

    val changeLanguageState: LiveData<ResultState<List<UpdateLanguageInputUseCase.State>>> = MediatorLiveData()

    val languageViewItemList: LiveData<List<ViewItem>> = listenerSources(languageSelected, languageListState, changeLanguageState) {

        val state = languageListState.get()

        if (state is ResultState.Start) {

            postValue(itemLoading)
            return@listenerSources
        }

        if (state !is ResultState.Success) {

            return@listenerSources
        }

        val languageSelected = languageSelected.value


        val listLanguage = state.data.map {

            LanguageViewItem(
                data = it,
                name = it.name,
                image = it.image,
                isSelected = it.id == languageSelected?.id
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

    val buttonInfo: LiveData<ButtonInfo> = listenerSources(languageOld, languageSelected, changeLanguageState, keyTranslateMap) {

        val keyTranslateMap = keyTranslateMap.get()

        val languageOld = languageOld.value
        val languageSelected = languageSelected.value

        val changeLanguageState = changeLanguageState.value

        val isSelected = languageOld?.id != languageSelected?.id

        val isClickable = isSelected && !changeLanguageState.isSuccess()

        val info = ButtonInfo(
            text = keyTranslateMap["action_confirm_change_language"].orEmpty(),
            isSelected = isSelected,
            isClickable = isClickable,
            isShowLoading = changeLanguageState != null && !changeLanguageState.isCompleted()
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

            it.doFailed {
                Log.d("tuanha", "changeLanguageInput: ", it)
            }

            this@LanguageViewModel.changeLanguageState.postValue(it)
        }
    }

    private fun ResultState<List<UpdateLanguageInputUseCase.State>>.toViewItem() = arrayListOf<ViewItem>().apply {

        val list = this@toViewItem.toSuccess()?.data ?: this@toViewItem.toRunning()?.data ?: return@apply

        val stateLast = list.lastOrNull()

        val keyTranslateMap = keyTranslateMap.value ?: return@apply

        if (stateLast?.value.orZero() >= UpdateLanguageInputUseCase.State.START.value) LanguageStateViewItem(
            data = UpdateLanguageInputUseCase.State.START.name,
            name = keyTranslateMap["message_start_sync"].orEmpty()
        ).let {

            add(it)
        }

        (if (stateLast?.value.orZero() == UpdateLanguageInputUseCase.State.SYNC_PHONETICS.value) LanguageStateViewItem(
            data = UpdateLanguageInputUseCase.State.SYNC_PHONETICS.name,
            name = keyTranslateMap["message_start_sync_phonetics"].orEmpty()
        ) else if (stateLast?.value.orZero() > UpdateLanguageInputUseCase.State.SYNC_PHONETICS.value) LanguageStateViewItem(
            data = UpdateLanguageInputUseCase.State.SYNC_PHONETICS.name,
            name = keyTranslateMap["message_completed_sync_phonetics"].orEmpty()
        ) else {
            null
        })?.let {

            add(it)
        }

        (if (stateLast?.value.orZero() == UpdateLanguageInputUseCase.State.SYNC_TRANSLATE.value) LanguageStateViewItem(
            data = UpdateLanguageInputUseCase.State.SYNC_TRANSLATE.name,
            name = keyTranslateMap["message_start_sync_translate"].orEmpty()
        ) else if (stateLast?.value.orZero() > UpdateLanguageInputUseCase.State.SYNC_TRANSLATE.value) LanguageStateViewItem(
            data = UpdateLanguageInputUseCase.State.SYNC_TRANSLATE.name,
            name = keyTranslateMap["message_completed_sync_translate"].orEmpty()
        ) else {
            null
        })?.let {

            add(it)
        }

        if (list.contains(UpdateLanguageInputUseCase.State.COMPLETED)) LanguageStateViewItem(
            data = UpdateLanguageInputUseCase.State.COMPLETED.name,
            name = keyTranslateMap["message_sync_completed"].orEmpty()
        ).let {

            add(it)
        }
    }

    data class ButtonInfo(
        val text: String,
        val isSelected: Boolean,
        val isClickable: Boolean,
        val isShowLoading: Boolean,
    )
}