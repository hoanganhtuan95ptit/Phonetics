package com.simple.phonetics.ui

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
import com.simple.phonetics.domain.usecase.key_translate.GetKeyTranslateAsyncUseCase
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
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isFailed
import com.simple.state.isStart

class ConfigViewModel(
    private val translateUseCase: TranslateUseCase,
    private val getVoiceAsyncUseCase: GetVoiceAsyncUseCase,
    private val getKeyTranslateAsyncUseCase: GetKeyTranslateAsyncUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase,
    private val getLanguageOutputAsyncUseCase: GetLanguageOutputAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val keyTranslateMap: LiveData<Map<String, String>> = mediatorLiveData {

        getKeyTranslateAsyncUseCase.execute().collect {

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
    val listPhoneViewItem: LiveData<List<OptionViewItem<String>>> = combineSources<List<OptionViewItem<String>>>(inputLanguage, phoneticSelect) {

        val language = inputLanguage.get()

        val phoneticSelect = phoneticSelect.get()

        language.listIpa.map {

            val code = it.code

            PhoneticCodeOptionViewItem(
                id = code,
                data = code,
                text = code,
                isSelect = code == phoneticSelect
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

            state.doSuccess {

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

    @VisibleForTesting
    val listTranslationViewItem: LiveData<List<OptionViewItem<Boolean>>> = combineSources<List<OptionViewItem<Boolean>>>(translateState, translateSelect, keyTranslateMap) {

        val translateState = translateState.get()
        val translateSelect = translateSelect.get()

        val keyTranslateMap = keyTranslateMap.get()


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
            keyTranslateMap["message_support_translate"]
        } else {
            keyTranslateMap["message_translate_download"]
        }


        val list = arrayListOf<OptionViewItem<Boolean>>()

        TranslationOptionViewItem(
            id = id,
            text = text.orEmpty(),
            isSelect = translateSelect == id
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
    val listVoiceSpeedViewItem: LiveData<List<VoiceSpeedViewItem>> = combineSources(listVoice, voiceSpeed, keyTranslateMap) {

        val listVoice = listVoice.get()
        val voiceSpeed = voiceSpeed.get()
        val keyTranslateMap = keyTranslateMap.get()

        if (listVoice.isEmpty()) {

            postValue(emptyList())
            return@combineSources
        }


        val list = arrayListOf<VoiceSpeedViewItem>()

        VoiceSpeedViewItem(
            start = 0f,
            end = 2f,

            text = keyTranslateMap["speed_lever"].orEmpty().replace("\$lever", "$voiceSpeed"),

            current = voiceSpeed
        ).let {

            list.add(it)
        }

        postDifferentValue(list)
    }


    val voiceSelect: LiveData<Int> = MediatorLiveData<Int>().apply {

        value = 0
    }

    val listVoiceViewItem: LiveData<List<OptionViewItem<Int>>> = combineSources(listVoice, voiceSelect, keyTranslateMap) {

        val listVoice = listVoice.get()
        val voiceSelect = voiceSelect.get()

        if (listVoice.isEmpty()) {

            postValue(emptyList())
            return@combineSources
        }

        listVoice.mapIndexed { index, voice ->


            VoiceOptionViewItem(
                id = "$index",
                data = voice,
                text = keyTranslateMap.get()["voice_index"].orEmpty().replace("\$index", "$index"),
                isSelect = voice == voiceSelect
            )
        }.let {

            postDifferentValue(it)
        }
    }


    val listConfig: LiveData<List<ViewItem>> = combineSources(listPhoneViewItem, listVoiceViewItem, listVoiceSpeedViewItem, listTranslationViewItem) {

        val list = arrayListOf<ViewItem>()

        listPhoneViewItem.getOrEmpty().find { it.isSelect }?.let {

            list.add(TextOptionViewItem("LIST_PHONE_VIEW_ITEM", it.text, false))
        }

        listTranslationViewItem.getOrEmpty().find { it.isSelect }?.let {

            list.add(TextOptionViewItem("LIST_TRANSLATION_VIEW_ITEM", it.text, false))
        }

        listVoiceSpeedViewItem.getOrEmpty().firstOrNull()?.let {

            list.add(TextOptionViewItem("LIST_TRANSLATION_VIEW_ITEM", it.text, false))
        }

        listVoiceViewItem.getOrEmpty().find { it.isSelect }?.let {

            list.add(TextOptionViewItem("LIST_VOICE_VIEW_ITEM", it.text, false))
        }

        postDifferentValue(list)
    }

    val listViewItem: LiveData<List<ViewItem>> = combineSources(keyTranslateMap, listPhoneViewItem, listVoiceViewItem, listTranslationViewItem, listVoiceSpeedViewItem) {

        val keyTranslateMap = keyTranslateMap.value ?: return@combineSources

        val list = arrayListOf<ViewItem>()


        listPhoneViewItem.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            list.add(TitleViewItem(text = keyTranslateMap["title_phonetic"].orEmpty()))

            list.addAll(it)
        }

        listTranslationViewItem.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            list.add(TitleViewItem(text = keyTranslateMap["title_translate"].orEmpty()))

            list.addAll(it)
        }

        listVoiceSpeedViewItem.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            list.add(TitleViewItem(text = keyTranslateMap["title_voice_speed"].orEmpty()))

            list.addAll(it)
        }

        listVoiceViewItem.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            list.add(TitleViewItem(text = keyTranslateMap["title_voice"].orEmpty()))

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