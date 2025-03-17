package com.simple.phonetics.ui.game.congratulations

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.ui.base.fragments.BaseViewModel

class GameCongratulationViewModel() : BaseViewModel() {

    @VisibleForTesting
    val number: LiveData<Int> = MediatorLiveData(0)

    val info: LiveData<Info> = combineSources(size, theme, translate, number) {

        val size = size.get()
        val theme = theme.get()
        val translate = translate.get()

        val number = "${number.get()}"

        val info = Info(
            title = translate["game_congratulation_screen_title"].orEmpty(),
            message = translate["game_congratulation_screen_message"].orEmpty()
                .replace("\$number", number)
                .with(number, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorPrimary)),

            button = ButtonInfo(
                text = translate["game_congratulation_screen_action"].orEmpty()
                    .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.colorOnPrimary)),
                background = Background(
                    backgroundColor = theme.colorPrimary,
                    cornerRadius = DP.DP_350
                )
            )
        )

        postDifferentValueIfActive(info)
    }

    fun updateNumber(it: Int) {

    }

    data class Info(
        val title: CharSequence,
        val message: CharSequence,

        val button: ButtonInfo
    )
}