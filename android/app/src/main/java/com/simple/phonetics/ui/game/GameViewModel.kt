package com.simple.phonetics.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.entities.Text
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import kotlin.math.max

class GameViewModel : BaseViewModel() {

    val text: LiveData<Text> = MediatorLiveData()

    val title: LiveData<CharSequence> = combineSources(translate) {

        val translate = translate.getOrEmpty()

        val title = translate["game_screen_title"].orEmpty()

        postDifferentValue(title)
    }

    val consecutiveCorrectAnswer: LiveData<Pair<Long, Boolean>> = MediatorLiveData()
    val consecutiveCorrectAnswerEvent: LiveData<Event<Pair<Long, Boolean>>> = consecutiveCorrectAnswer.toEvent()


    fun updateText(it: Text?) {

        text.postDifferentValue(it ?: Text("", Text.Type.IPA))
    }

    fun updateAnswerCorrect(isCorrect: Boolean) {

        val count = if (isCorrect) {

            max(0, consecutiveCorrectAnswer.value?.first ?: 0) + 1L
        } else {

            -System.currentTimeMillis()
        }

        consecutiveCorrectAnswer.postDifferentValue(count to (count % 5 == 0L))
    }
}