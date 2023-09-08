package com.simple.phonetics.ui.phonetics.config

import android.speech.tts.Voice
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.adapter.ViewItemCloneable
import com.simple.coreapp.ui.base.viewmodels.BaseViewModel
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.toText
import com.simple.state.ResultState
import com.simple.state.isFailed
import com.simple.state.isStart
import com.simple.phonetics.R
import com.simple.phonetics.domain.entities.PhoneticsCode
import com.simple.phonetics.ui.adapters.OptionViewItem
import com.simple.phonetics.ui.adapters.TextOptionViewItem
import com.simple.phonetics.ui.adapters.TitleViewItem
import com.simple.phonetics.ui.phonetics.config.adapters.PhoneticCodeOptionViewItem
import com.simple.phonetics.ui.phonetics.config.adapters.TranslationOptionViewItem
import com.simple.phonetics.ui.phonetics.config.adapters.VoiceOptionViewItem
import com.simple.phonetics.ui.phonetics.config.adapters.VoiceSpeedViewItem

class PhoneticsConfigViewModel : BaseViewModel() {

    val phoneticSelect: LiveData<PhoneticsCode> = MediatorLiveData<PhoneticsCode>().apply {

        value = PhoneticsCode.US
    }

    @VisibleForTesting
    val listPhoneViewItem: LiveData<List<OptionViewItem<PhoneticsCode>>> = combineSources<List<OptionViewItem<PhoneticsCode>>>(phoneticSelect) {

        val phoneticSelect = phoneticSelect.get()

        PhoneticsCode.values().map {

            PhoneticCodeOptionViewItem(it.value, it).refresh(it == phoneticSelect)
        }.let {

            postDifferentValue(it)
        }
    }.apply {

        postValue(emptyList())
    }


    @VisibleForTesting
    val translateState: LiveData<ResultState<Boolean>> = MediatorLiveData<ResultState<Boolean>>().apply {

        value = ResultState.Start
    }

    val translateSelect: LiveData<String> = MediatorLiveData<String>().apply {

        value = "0"
    }

    val listTranslationViewItem: LiveData<List<OptionViewItem<Boolean>>> = combineSources<List<OptionViewItem<Boolean>>>(translateState, translateSelect) {

        val translateState = translateState.get()

        val translateSelect = translateSelect.get()


        if (translateState.isFailed()) {

            postDifferentValue(emptyList())
            return@combineSources
        }


        val id = if (translateState.isStart()) "" else "0"


        val list = arrayListOf<OptionViewItem<Boolean>>()

        list.add(TranslationOptionViewItem(id).refresh(translateSelect == id))

        postDifferentValue(list)
    }.apply {

        postValue(emptyList())
    }


    @VisibleForTesting
    val listVoice: LiveData<List<Voice>> = MediatorLiveData()


    @VisibleForTesting
    val voiceSpeed: LiveData<Float> = MediatorLiveData<Float>().apply {

        value = 1f
    }

    val listVoiceSpeedViewItem: LiveData<List<VoiceSpeedViewItem>> = combineSources(listVoice, voiceSpeed) {

        val list = arrayListOf<VoiceSpeedViewItem>()

        list.add(VoiceSpeedViewItem(start = 0f, end = 2f, current = voiceSpeed.get()))

        postDifferentValue(list)
    }


    @VisibleForTesting
    val voiceSelect: LiveData<String> = MediatorLiveData<String>().apply {

        value = "0"
    }

    val listVoiceViewItem: LiveData<List<OptionViewItem<Voice>>> = combineSources(listVoice, voiceSelect) {

        val voiceSelect = voiceSelect.get()

        listVoice.getOrEmpty().mapIndexed { index, voice ->

            val id = "$index"

            VoiceOptionViewItem(id, voice).refresh(id == voiceSelect)
        }.let {

            postDifferentValue(it)
        }
    }


    val listConfig: LiveData<List<ViewItemCloneable>> = combineSources(listPhoneViewItem, listVoiceViewItem, listVoiceSpeedViewItem, listTranslationViewItem) {

        val list = arrayListOf<ViewItemCloneable>()

        listPhoneViewItem.getOrEmpty().find { it.isSelect }?.let {

            list.add(TextOptionViewItem("listPhoneViewItem", it.text, false))
        }

        listTranslationViewItem.getOrEmpty().find { it.isSelect }?.let {

            list.add(TextOptionViewItem("listTranslationViewItem", it.text, false))
        }

        listVoiceSpeedViewItem.getOrEmpty().map { it.clone() }.firstOrNull()?.let {

            list.add(TextOptionViewItem("listTranslationViewItem", "Speed ${it.current}".toText(), false))
        }

        listVoiceViewItem.getOrEmpty().find { it.isSelect }?.let {

            list.add(TextOptionViewItem("listVoiceViewItem", it.text, false))
        }

        postDifferentValue(list)
    }


    val listViewItem: LiveData<List<ViewItemCloneable>> = combineSources(listPhoneViewItem, listVoiceViewItem, listTranslationViewItem, listVoiceSpeedViewItem) {

        val list = arrayListOf<ViewItemCloneable>()


        listPhoneViewItem.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            list.add(TitleViewItem(R.string.title_phonetic).refresh())

            list.addAll(it)
        }

        listTranslationViewItem.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            list.add(TitleViewItem(R.string.title_translate).refresh())

            list.addAll(it)
        }

        listVoiceSpeedViewItem.getOrEmpty().map { it.clone() }.takeIf { it.isNotEmpty() }?.let {

            list.add(TitleViewItem(R.string.title_voice_speed).refresh())

            list.addAll(it)
        }

        listVoiceViewItem.getOrEmpty().takeIf { it.isNotEmpty() }?.let {

            list.add(TitleViewItem(R.string.title_voice).refresh())

            list.addAll(it)
        }

        postDifferentValue(list)
    }


    fun updateVoice(listVoice: List<Voice>) {

        this.listVoice.postValue(listVoice)
    }

    fun updateVoiceSpeed(current: Float) {

        this.voiceSpeed.postDifferentValue(current)
    }

    fun updateVoiceSelect(id: String) {

        this.voiceSelect.postDifferentValue(id)
    }

    fun updateTranslation(id: String) {

        this.translateSelect.postDifferentValue(if (translateSelect.value.isNullOrBlank()) id else "")
    }

    fun updatePhoneticSelect(data: PhoneticsCode) {

        this.phoneticSelect.postDifferentValue(data)
    }

    fun updateTranslateState(state: ResultState<Boolean>) {

        this.translateState.postDifferentValue(state)
    }
}