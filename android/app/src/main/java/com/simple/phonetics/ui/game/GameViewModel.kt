package com.simple.phonetics.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.entities.Text
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.phonetics.utils.spans.RoundedBackgroundSpan
import kotlin.math.max

class GameViewModel : BaseViewModel() {

    val text: LiveData<Text> = MediatorLiveData()

    val title: LiveData<CharSequence> = combineSourcesWithDiff(theme, translate, text) {

        val theme = theme.get()
        val translate = translate.getOrEmpty()

        val text = text.get()

        val title = if (text.text.isNotEmpty()) {
            (translate["game_screen_title"].orEmpty() + " " + text.text)
                .with(text.text, Bold, RoundedBackgroundSpan(backgroundColor = theme.getOrTransparent("colorErrorVariant"), textColor = theme.getOrTransparent("colorOnErrorVariant")))
        } else {
            translate["game_screen_title"].orEmpty()
        }

        postValue(title)
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

        consecutiveCorrectAnswer.postValue(count to (count % 5 == 0L))
    }
}