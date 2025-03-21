package com.simple.phonetics.ui.game.items

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.base.fragments.BaseViewModel

abstract class GameItemViewModel : BaseViewModel() {


    val inputLanguage: LiveData<Language> = MediatorLiveData()

    val outputLanguage: LiveData<Language> = MediatorLiveData()


    val phoneticCodeSelected: LiveData<String> = MediatorLiveData()


    val listenEnable: LiveData<Boolean> = MediatorLiveData()

    val resourceSelected: LiveData<Word.Resource> = MediatorLiveData()

    val consecutiveCorrectAnswerEvent: LiveData<Event<Pair<Long, Boolean>>> = MediatorLiveData()

    fun updateInputLanguage(language: Language) {

        inputLanguage.postDifferentValue(language)
    }

    fun updateOutputLanguage(language: Language) {

        outputLanguage.postDifferentValue(language)
    }


    fun updatePhoneticCodeSelected(code: String) {

        phoneticCodeSelected.postDifferentValue(code)
    }


    fun updateListenerEnable(it: Boolean) {

        listenEnable.postDifferentValue(it)
    }

    fun updateResourceSelected(it: Word.Resource) {

        resourceSelected.postDifferentValue(it)
    }

    fun updateConsecutiveCorrectAnswer(event: Event<Pair<Long, Boolean>>) {

        consecutiveCorrectAnswerEvent.postDifferentValue(event)
    }


    data class StateInfo(
        val anim: Int? = null,

        val title: CharSequence,
        val message: CharSequence,

        val background: Background = DEFAULT_BACKGROUND,

        val positive: com.simple.coreapp.utils.ext.ButtonInfo? = null,
    )

    data class ButtonInfo(
        val text: CharSequence,
        val isClickable: Boolean,

        val background: Background = DEFAULT_BACKGROUND
    )
}