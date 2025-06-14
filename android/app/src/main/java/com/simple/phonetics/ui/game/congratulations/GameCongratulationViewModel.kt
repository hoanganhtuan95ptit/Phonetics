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
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.R
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.getOrTransparent

class GameCongratulationViewModel : BaseViewModel() {

    @VisibleForTesting
    val number: LiveData<Long> = MediatorLiveData(0L)

    val info: LiveData<Info> = combineSources(size, theme, translate, number) {

        val theme = theme.get()
        val translate = translate.get()

        val number = number.get()
        val numberStr = "$number"

        val anim = listOf(
            R.raw.anim_congratulations_1,
            R.raw.anim_congratulations_2,
            R.raw.anim_congratulations_3,
            R.raw.anim_congratulations_4,
            R.raw.anim_congratulations_5
        ).random()

        val title = if (number >= 25 && translate.containsKey("game_congratulation_screen_title_4")) {
            translate["game_congratulation_screen_title_4"].orEmpty()
        } else if (number >= 20 && translate.containsKey("game_congratulation_screen_title_3")) {
            translate["game_congratulation_screen_title_3"].orEmpty()
        } else if (number >= 15 && translate.containsKey("game_congratulation_screen_title_2")) {
            translate["game_congratulation_screen_title_2"].orEmpty()
        } else if (number >= 10 && translate.containsKey("game_congratulation_screen_title_1")) {
            translate["game_congratulation_screen_title_1"].orEmpty()
        } else {
            translate["game_congratulation_screen_title"].orEmpty()
        }

        val message = if (number >= 25 && translate.containsKey("game_congratulation_screen_message_4")) {
            translate["game_congratulation_screen_message_4"].orEmpty()
        } else if (number >= 20 && translate.containsKey("game_congratulation_screen_message_3")) {
            translate["game_congratulation_screen_message_3"].orEmpty()
        } else if (number >= 15 && translate.containsKey("game_congratulation_screen_message_2")) {
            translate["game_congratulation_screen_message_2"].orEmpty()
        } else if (number >= 10 && translate.containsKey("game_congratulation_screen_message_1")) {
            translate["game_congratulation_screen_message_1"].orEmpty()
        } else {
            translate["game_congratulation_screen_message"].orEmpty()
        }

        val info = Info(
            anim = anim,
            title = title
                .with(ForegroundColorSpan(theme.getOrTransparent("colorOnSurface"))),
            message = message
                .replace("\$number", numberStr)
                .with(ForegroundColorSpan(theme.getOrTransparent("colorOnSurface")))
                .with(numberStr, StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.getOrTransparent("colorPrimary"))),

            button = ButtonInfo(
                text = translate["game_congratulation_screen_action"].orEmpty()
                    .with(StyleSpan(Typeface.BOLD), ForegroundColorSpan(theme.getOrTransparent("colorOnPrimary"))),
                background = Background(
                    backgroundColor = theme.getOrTransparent("colorPrimary"),
                    cornerRadius = DP.DP_350
                )
            )
        )

        postDifferentValueIfActive(info)
    }

    fun updateNumber(it: Long) {

        number.postDifferentValue(it)
    }

    data class Info(
        val anim: Int,

        val title: CharSequence,
        val message: CharSequence,

        val button: ButtonInfo
    )
}