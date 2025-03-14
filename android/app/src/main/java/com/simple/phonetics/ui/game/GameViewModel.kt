package com.simple.phonetics.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.getOrEmpty
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.ui.base.fragments.BaseViewModel

class GameViewModel : BaseViewModel() {

    val title: LiveData<CharSequence> = combineSources(translate) {

        val translate = translate.getOrEmpty()

        val title = translate["game_screen_title"].orEmpty()

        postDifferentValue(title)
    }

    val consecutiveCorrectAnswers: LiveData<Int> = MediatorLiveData()


    fun updateAnswerCorrect(isCorrect: Boolean) {

        val count = if (isCorrect) {

            consecutiveCorrectAnswers.value.orZero() + 1
        } else {

            0
        }

        consecutiveCorrectAnswers.postDifferentValue(count)
    }
}