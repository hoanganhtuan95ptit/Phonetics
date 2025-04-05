package com.simple.phonetics.ui.home.view.review

import android.text.style.ForegroundColorSpan
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import kotlin.math.absoluteValue

class ReviewHomeViewModel(
    private val getPhoneticsHistoryAsyncUseCase: GetPhoneticsHistoryAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val rate: LiveData<Rate> = MediatorLiveData()

    @VisibleForTesting
    val show: LiveData<Boolean> = MediatorLiveData()

    val historyList: LiveData<List<Sentence>> = mediatorLiveData {

        postDifferentValue(getPhoneticsHistoryAsyncUseCase.execute(null).firstOrNull().orEmpty())
    }

    @VisibleForTesting
    val rateInfo: LiveData<RateInfo> = combineSources(theme, translate, rate, show, historyList) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val rate = rate.value ?: return@combineSources
        val show = show.value ?: return@combineSources
        val historyList = historyList.value ?: return@combineSources

        val isValidate = rate.date == 0 || (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - rate.date).absoluteValue >= 3

        // nếu người dùng đã xác nhận mở rate
        if (show == false || historyList.isEmpty() || rate.status == Rate.Status.OPEN_RATE.value || !isValidate) {

            postDifferentValue(RateInfo(show = false))
            return@combineSources
        }

        RateInfo(
            show = true,
            anim = R.raw.anim_rate,
            title = translate["rate_title"].orEmpty(),
            message = translate["rate_message"].orEmpty(),
            positive = ButtonInfo(
                text = translate["rate_action_positive"].orEmpty().with(ForegroundColorSpan(theme.colorOnPrimary)),
                background = Background(
                    backgroundColor = theme.colorPrimary,
                    cornerRadius = DP.DP_16
                )
            ),
            negative = ButtonInfo(
                text = translate["rate_action_negative"].orEmpty().with(ForegroundColorSpan(theme.colorOnSurfaceVariant)),
                background = Background(
                    backgroundColor = theme.colorBackground,
                    strokeColor = theme.colorOnSurfaceVariant,
                    strokeWidth = DP.DP_1,
                    cornerRadius = DP.DP_16
                )
            ),
        ).apply {

            postDifferentValue(this)
        }
    }

    val rateInfoEvent: LiveData<Event<RateInfo>> = rateInfo.toEvent()


    fun show() {
        show.postDifferentValue(true)
    }

    fun updateRate(rate: Rate?) {
        this.rate.postDifferentValue(rate ?: Rate())
    }

    data class Rate(
        val date: Int = 0,
        val status: Int = Status.NONE.value
    ) {

        enum class Status(val value: Int) {
            NONE(0), OPEN_RATE(1), DISMISS(2),
        }
    }

    data class RateInfo(
        val show: Boolean,

        val anim: Int = 0,

        val title: CharSequence = "",
        val message: CharSequence = "",

        val positive: ButtonInfo? = null,
        val negative: ButtonInfo? = null
    )
}