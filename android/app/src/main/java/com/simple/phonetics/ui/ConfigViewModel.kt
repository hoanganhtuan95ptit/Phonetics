package com.simple.phonetics.ui

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModels.BaseViewModel
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.ui.adapters.TextViewItem
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.round.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.Id
import com.simple.phonetics.Id.TRANSLATE
import com.simple.phonetics.domain.usecase.TranslateUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetVoiceAsyncUseCase
import com.simple.phonetics.entities.Language
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
    val phoneViewItemList: LiveData<List<ViewItem>> = combineSources<List<ViewItem>>(theme, inputLanguage, phoneticSelect) {

        val theme = theme.get()

        val language = inputLanguage.get()
        val phoneticSelect = phoneticSelect.get()

        language.listIpa.map {

            val isSelect = it.code == phoneticSelect

            createOptionViewItem(
                id = Id.IPA + "_" + it.code,
                data = it.code to isSelect,
                text = it.name.with(ForegroundColorSpan(if (isSelect) theme.colorPrimary else theme.colorOnSurface)),
                strokeColor = if (isSelect) theme.colorPrimary else theme.colorOnSurface,
                backgroundColor = if (isSelect) theme.colorPrimaryVariant else Color.TRANSPARENT
            )
        }.let {

            postDifferentValue(it)
        }
    }.apply {

        postValue(emptyList())
    }


    @VisibleForTesting
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

    @VisibleForTesting
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
    val translateViewItemList: LiveData<List<ViewItem>> = combineSources<List<ViewItem>>(theme, translate, translateState, translateSelect) {

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


        val list = arrayListOf<ViewItem>()

        val isSelect = translateSelect == id

        createOptionViewItem(
            id = "$TRANSLATE-$id",
            data = id to isSelect,
            text = text.orEmpty().with(ForegroundColorSpan(if (isSelect) theme.colorPrimary else theme.colorOnSurface)),
            strokeColor = if (isSelect) theme.colorPrimary else theme.colorOnSurface,
            backgroundColor = if (isSelect) theme.colorPrimaryVariant else Color.TRANSPARENT
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
    val voiceSpeedViewItemList: LiveData<List<VoiceSpeedViewItem>> = combineSources(theme, translate, listVoice, voiceSpeed) {

        val translate = translate.get()

        val listVoice = listVoice.get()
        val voiceSpeed = voiceSpeed.get()

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

    val voiceViewItemList: LiveData<List<ViewItem>> = combineSources(theme, listVoice, voiceSelect, translate) {

        val theme = theme.get()
        val translate = translate.get()

        val listVoice = listVoice.get()
        val voiceSelect = voiceSelect.get()

        if (listVoice.isEmpty()) {

            postValue(emptyList())
            return@combineSources
        }

        listVoice.mapIndexed { index, voice ->

            val isSelect = voice == voiceSelect

            createOptionViewItem(
                id = "${Id.VOICE}-${voice}",
                data = voice to isSelect,
                text = translate["voice_index"].orEmpty()
                    .replace("\$index", "$index")
                    .with(ForegroundColorSpan(if (isSelect) theme.colorPrimary else theme.colorOnSurface)),
                strokeColor = if (isSelect) theme.colorPrimary else theme.colorOnSurface,
                backgroundColor = if (isSelect) theme.colorPrimaryVariant else Color.TRANSPARENT
            )
        }.let {

            postDifferentValue(it)
        }
    }


    val listConfig: LiveData<List<ViewItem>> = combineSources(theme, phoneViewItemList, voiceViewItemList, voiceSpeedViewItemList, translateViewItemList) {

        val theme = theme.value ?: return@combineSources

        val list = arrayListOf<ViewItem>()

        val textColor = theme.colorOnSurface
        val strokeColor = theme.colorOnSurface
        val backgroundColor = Color.TRANSPARENT

        phoneViewItemList.getOrEmpty().filterIsInstance<TextViewItem>().find { it.data.asObjectOrNull<Pair<String, Boolean>>()?.second == true }?.let {

            createOptionViewItem(
                id = "LIST_PHONE_VIEW_ITEM",
                text = it.text.with(ForegroundColorSpan(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        translateViewItemList.getOrEmpty().filterIsInstance<TextViewItem>().find { it.data.asObjectOrNull<Pair<String, Boolean>>()?.second == true }?.let {

            createOptionViewItem(
                id = "LIST_TRANSLATE_VIEW_ITEM",
                text = it.text.with(ForegroundColorSpan(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        voiceSpeedViewItemList.getOrEmpty().firstOrNull()?.let {

            createOptionViewItem(
                id = "LIST_VOICE_SPEED_VIEW_ITEM",
                text = it.text.with(ForegroundColorSpan(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        voiceViewItemList.getOrEmpty().filterIsInstance<TextViewItem>().find { it.data.asObjectOrNull<Pair<String, Boolean>>()?.second == true }?.let {

            createOptionViewItem(
                id = "LIST_VOICE_VIEW_ITEM",
                text = it.text.with(ForegroundColorSpan(textColor)),
                strokeColor = strokeColor,
                backgroundColor = backgroundColor
            )
        }?.let {

            list.add(it)
        }

        postDifferentValue(list)
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(theme, translate, phoneViewItemList, voiceViewItemList, translateViewItemList, voiceSpeedViewItemList) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val list = arrayListOf<ViewItem>()


        phoneViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_phonetic"].orEmpty()

            list.add(TextViewItem(id = "title_phonetic", text = text.with(ForegroundColorSpan(theme.colorOnSurface)), padding = Padding(top = DP.DP_16, bottom = DP.DP_4)))

            list.addAll(it)
        }

        translateViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_translate"].orEmpty()

            list.add(TextViewItem(id = "title_translate", text = text.with(ForegroundColorSpan(theme.colorOnSurface)), padding = Padding(top = DP.DP_16, bottom = DP.DP_4)))

            list.addAll(it)
        }

        voiceSpeedViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_voice_speed"].orEmpty()

            list.add(TextViewItem(id = "title_voice_speed", text = text.with(ForegroundColorSpan(theme.colorOnSurface)), padding = Padding(top = DP.DP_16, bottom = DP.DP_4)))

            list.addAll(it)
        }

        voiceViewItemList.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            val text = translate["title_voice"].orEmpty()

            list.add(TextViewItem(id = "title_voice", text = text.with(ForegroundColorSpan(theme.colorOnSurface)), padding = Padding(top = DP.DP_16, bottom = DP.DP_4)))

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

    private fun createOptionViewItem(
        id: String,

        text: CharSequence,

        data: Any? = null,

        strokeColor: Int,
        backgroundColor: Int
    ) = TextViewItem(
        id = id,
        data = data,
        text = text,
        size = Size(
            width = ViewGroup.LayoutParams.WRAP_CONTENT,
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        ),
        margin = Margin(
            top = DP.DP_4,
            right = DP.DP_8,
            bottom = DP.DP_4
        ),
        padding = Padding(
            top = DP.DP_4,
            bottom = DP.DP_4,
            left = DP.DP_16,
            right = DP.DP_16
        ),
        background = Background(
            cornerRadius = DP.DP_100,
            strokeWidth = DP.DP_1,
            strokeColor = strokeColor,
            backgroundColor = backgroundColor
        )
    )
}

