package com.simple.phonetics.ui.game.congratulations

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.phonetics.R
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary

class GameCongratulationViewModel : BaseViewModel() {

    @VisibleForTesting
    val number: LiveData<Long> = MediatorLiveData(0L)

    val info: LiveData<Info> = combineSourcesWithDiff(size, theme, translate, number) {

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
                .with(ForegroundColor(theme.colorOnSurface)),
            message = message
                .replace("\$number", numberStr)
                .with(ForegroundColor(theme.colorOnSurface))
                .with(numberStr, Bold, ForegroundColor(theme.colorPrimary)),

            button = ButtonInfo(
                text = translate["game_congratulation_screen_action"].orEmpty()
                    .with(Bold, ForegroundColor(theme.colorOnPrimary)),
                background = Background(
                    backgroundColor = theme.colorPrimary,
                    cornerRadius = DP.DP_350
                )
            )
        )

        postValueIfActive(info)
    }

    fun updateNumber(it: Long) {

        number.postValue(it)
    }

    data class Info(
        val anim: Int,

        val title: RichText,
        val message: RichText,

        val button: ButtonInfo
    )
}