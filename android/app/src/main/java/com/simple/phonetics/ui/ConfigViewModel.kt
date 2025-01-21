package com.simple.phonetics.ui

import android.graphics.Color
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModels.BaseViewModel
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.domain.usecase.TranslateUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetVoiceAsyncUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.adapters.OptionViewItem
import com.simple.phonetics.ui.adapters.TextOptionViewItem
import com.simple.phonetics.ui.adapters.TitleViewItem
import com.simple.phonetics.ui.config.adapters.PhoneticCodeOptionViewItem
import com.simple.phonetics.ui.config.adapters.TranslationOptionViewItem
import com.simple.phonetics.ui.config.adapters.VoiceOptionViewItem
import com.simple.phonetics.ui.config.adapters.VoiceSpeedViewItem
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.appTheme
import com.simple.phonetics.utils.appTranslate
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isFailed
import com.simple.state.isStart
import com.simple.state.toSuccess

class ConfigViewModel(
    private val translateUseCase: TranslateUseCase,
    private val getVoiceAsyncUseCase: GetVoiceAsyncUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase,
    private val getLanguageOutputAsyncUseCase: GetLanguageOutputAsyncUseCase
) : BaseViewModel() {

    val theme: LiveData<AppTheme> = mediatorLiveData {

        appTheme.collect {

            postDifferentValue(it)
        }
    }

    @VisibleForTesting
    val translate: LiveData<Map<String, String>> = mediatorLiveData {

        appTranslate.collect {

            postDifferentValue(it)
        }
    }


    val inputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageInputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val outputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageOutputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }


    val phoneticSelect: LiveData<String> = combineSources(inputLanguage) {

        val language = inputLanguage.get()

        postValue(language.listIpa.first().code)
    }

    @VisibleForTesting
    val listPhoneViewItem: LiveData<List<OptionViewItem<String>>> = combineSources<List<OptionViewItem<String>>>(theme, inputLanguage, phoneticSelect) {

        val theme = theme.value ?: return@combineSources
        val language = inputLanguage.get()
        val phoneticSelect = phoneticSelect.get()

        language.listIpa.map {

            val isSelect = it.code == phoneticSelect

            PhoneticCodeOptionViewItem(
                id = it.code,
                data = it.code,
                text = it.name,
                isSelect = isSelect,
                textColor = if (isSelect) {
                    theme.colorPrimary
                } else {
                    theme.colorOnSurface
                },
                strokeColor = if (isSelect) {
                    theme.colorPrimary
                } else {
                    theme.colorOnSurface
                },
                backgroundColor = if (isSelect) {
                    theme.colorPrimaryVariant
                } else {
                    Color.TRANSPARENT
                }
            )
        }.let {

            postDifferentValue(it)
        }
    }.apply {

        postValue(emptyList())
    }


    val translateState: LiveData<ResultState<Boolean>> = combineSources(inputLanguage, outputLanguage) {

        postValue(ResultState.Start)

        val inputLanguageCode = inputLanguage.get().id

        val outputLanguageCode = outputLanguage.get().id

        translateUseCase.execute(TranslateUseCase.Param(listOf("hello"), inputLanguageCode, outputLanguageCode)).let { state ->

            state.toSuccess()?.data?.firstOrNull()?.translateState?.doSuccess {

                postValue(ResultState.Success(true))
            }

            state.doFailed {

                postValue(ResultState.Failed(it))
            }
        }
    }

    val translateSelect: LiveData<String> = MediatorLiveData<String>().apply {

        value = "0"
    }

    val translateEnable: LiveData<Boolean> = combineSources(translateState, translateSelect) {

        val translateState = translateState.get()
        val translateSelect = translateSelect.get()

        if (translateState.isFailed()) {

            postDifferentValue(false)
            return@combineSources
        }

        if (translateState !is ResultState.Success) {

            return@combineSources
        }

        postDifferentValue(translateSelect.isNotBlank())
    }

    @VisibleForTesting
    val translateViewItemList: LiveData<List<OptionViewItem<Boolean>>> = combineSources<List<OptionViewItem<Boolean>>>(theme, translate, translateState, translateSelect) {

        val theme = theme.get()
        val translate = translate.get()

        val translateState = translateState.get()
        val translateSelect = translateSelect.get()

        if (translateState.isFailed()) {

            postDifferentValue(emptyList())
            return@combineSources
        }


        val id = if (translateState.isStart()) {
            ""
        } else {
            "0"
        }

        val text = if (id.isNotBlank()) {
            translate["message_support_translate"]
        } else {
            translate["message_translate_download"]
        }


        val list = arrayListOf<OptionViewItem<Boolean>>()

        val isSelect = translateSelect == id

        TranslationOptionViewItem(
            id = id,
            text = text.orEmpty(),
            isSelect = isSelect,
            textColor = if (isSelect) {
                theme.colorPrimary
            } else {
                theme.colorOnSurface
            },
            strokeColor = if (isSelect) {
                theme.colorPrimary
            } else {
                theme.colorOnSurface
            },
            backgroundColor = if (isSelect) {
                theme.colorPrimaryVariant
            } else {
                Color.TRANSPARENT
            }
        ).let {

            list.add(it)
        }

        postDifferentValue(list)
    }.apply {

        postValue(emptyList())
    }


    val voiceState: LiveData<ResultState<List<Int>>> = mediatorLiveData {

        postValue(ResultState.Start)

        getVoiceAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val listVoice: LiveData<List<Int>> = combineSources(voiceState) {

        val state = voiceState.get()

        state.doSuccess {

            postDifferentValue(it)
        }

        state.doFailed {

            postDifferentValue(emptyList())
        }
    }


    val voiceSpeed: LiveData<Float> = MediatorLiveData<Float>().apply {

        value = 1f
    }

    @VisibleForTesting
    val listVoiceSpeedViewItem: LiveData<List<VoiceSpeedViewItem>> = combineSources(listVoice, voiceSpeed, translate) {

        val listVoice = listVoice.get()
        val voiceSpeed = voiceSpeed.get()
        val translate = translate.get()

        if (listVoice.isEmpty()) {

            postValue(emptyList())
            return@combineSources
        }


        val list = arrayListOf<VoiceSpeedViewItem>()

        VoiceSpeedViewItem(
            start = 0f,
            end = 2f,

            text = translate["speed_lever"].orEmpty().replace("\$lever", "$voiceSpeed"),

            current = voiceSpeed
        ).let {

            list.add(it)
        }

        postDifferentValue(list)
    }


    val voiceSelect: LiveData<Int> = MediatorLiveData<Int>().apply {

        value = 0
    }

    val listVoiceViewItem: LiveData<List<OptionViewItem<Int>>> = combineSources(theme, listVoice, voiceSelect, translate) {

        val theme = theme.get()
        val listVoice = listVoice.get()
        val voiceSelect = voiceSelect.get()

        if (listVoice.isEmpty()) {

            postValue(emptyList())
            return@combineSources
        }

        listVoice.mapIndexed { index, voice ->

            val isSelect = voice == voiceSelect

            VoiceOptionViewItem(
                id = "$index",
                data = voice,
                text = translate.get()["voice_index"].orEmpty().replace("\$index", "$index"),
                isSelect = voice == voiceSelect,
                textColor = if (isSelect) {
                    theme.colorPrimary
                } else {
                    theme.colorOnSurface
                },
                strokeColor = if (isSelect) {
                    theme.colorPrimary
                } else {
                    theme.colorOnSurface
                },
                backgroundColor = if (isSelect) {
                    theme.colorPrimaryVariant
                } else {
                    Color.TRANSPARENT
                }
            )
        }.let {

            postDifferentValue(it)
        }
    }


    val listConfig: LiveData<List<ViewItem>> = combineSources(theme, listPhoneViewItem, listVoiceViewItem, listVoiceSpeedViewItem, translateViewItemList) {

        val theme = theme.value ?: return@combineSources

        val list = arrayListOf<ViewItem>()

        val textColor = theme.colorOnSurface
        val strokeColor = theme.colorOnSurface
        val backgroundColor = Color.TRANSPARENT

        listPhoneViewItem.getOrEmpty().find { it.isSelect }?.let {

            list.add(TextOptionViewItem("LIST_PHONE_VIEW_ITEM", it.text, false, textColor = textColor, strokeColor = strokeColor, backgroundColor = backgroundColor))
        }

        translateViewItemList.getOrEmpty().find { it.isSelect }?.let {

            list.add(TextOptionViewItem("LIST_TRANSLATION_VIEW_ITEM", it.text, false, textColor = textColor, strokeColor = strokeColor, backgroundColor = backgroundColor))
        }

        listVoiceSpeedViewItem.getOrEmpty().firstOrNull()?.let {

            list.add(TextOptionViewItem("LIST_TRANSLATION_VIEW_ITEM", it.text, false, textColor = textColor, strokeColor = strokeColor, backgroundColor = backgroundColor))
        }

        listVoiceViewItem.getOrEmpty().find { it.isSelect }?.let {

            list.add(TextOptionViewItem("LIST_VOICE_VIEW_ITEM", it.text, false, textColor = textColor, strokeColor = strokeColor, backgroundColor = backgroundColor))
        }

        postDifferentValue(list)
    }

    val listViewItem: LiveData<List<ViewItem>> = combineSources(theme, translate, listPhoneViewItem, listVoiceViewItem, translateViewItemList, listVoiceSpeedViewItem) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val list = arrayListOf<ViewItem>()


        listPhoneViewItem.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_phonetic"].orEmpty()

            list.add(TitleViewItem(text = text, textColor = theme.colorOnSurface))

            list.addAll(it)
        }

        translateViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_translate"].orEmpty()

            list.add(TitleViewItem(text = text, textColor = theme.colorOnSurface))

            list.addAll(it)
        }

        listVoiceSpeedViewItem.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_voice_speed"].orEmpty()

            list.add(TitleViewItem(text = text, textColor = theme.colorOnSurface))

            list.addAll(it)
        }

        listVoiceViewItem.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_voice"].orEmpty()

            list.add(TitleViewItem(text = text, textColor = theme.colorOnSurface))

            list.addAll(it)
        }

        postDifferentValue(list)
    }


    fun updateVoiceSpeed(current: Float) {

        this.voiceSpeed.postDifferentValue(current)
    }

    fun updateVoiceSelect(id: Int) {

        this.voiceSelect.postDifferentValue(id)
    }

    fun updateTranslation(id: String) {

        this.translateSelect.postDifferentValue(if (translateSelect.value.isNullOrBlank()) id else "")
    }

    fun updatePhoneticSelect(data: String) {

        this.phoneticSelect.postDifferentValue(data)
    }
}

