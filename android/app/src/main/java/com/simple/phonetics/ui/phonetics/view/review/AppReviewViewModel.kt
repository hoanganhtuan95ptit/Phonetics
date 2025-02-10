package com.simple.phonetics.ui.phonetics.view.review

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.utils.extentions.Event
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.base.CommonViewModel
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import kotlin.math.absoluteValue

class AppReviewViewModel(
    private val getPhoneticsHistoryAsyncUseCase: GetPhoneticsHistoryAsyncUseCase
) : CommonViewModel() {

    @VisibleForTesting
    val historyList: LiveData<List<Sentence>> = mediatorLiveData {

        postDifferentValue(getPhoneticsHistoryAsyncUseCase.execute(null).firstOrNull().orEmpty())
    }

    @VisibleForTesting
    val rate: LiveData<Rate> = MediatorLiveData()


    val rateEnable: LiveData<Boolean> = combineSources(historyList) {

        postDifferentValue(historyList.value.orEmpty().isNotEmpty())
    }

    @VisibleForTesting
    val rateInfo: LiveData<RateInfo> = combineSources(translate, rate, rateEnable) {

        val translate = translate.value ?: return@combineSources

        val rate = rate.value ?: return@combineSources
        val rateEnable = rateEnable.value ?: return@combineSources

        if (!rateEnable) {

            postDifferentValue(RateInfo(show = false))
            return@combineSources
        }

        // nếu người dùng đã xác nhận mở rate
        if (rate.status == Rate.Status.OPEN_RATE.value) {

            postDifferentValue(RateInfo(show = false))
            return@combineSources
        }

        if (rate.date == 0 || (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - rate.date).absoluteValue >= 3) RateInfo(
            show = true,
            image = R.raw.anim_rate,
            title = translate["rate_title"].orEmpty(),
            message = translate["rate_message"].orEmpty(),
            positive = translate["rate_action_positive"].orEmpty(),
            negative = translate["rate_action_negative"].orEmpty(),
        ).apply {

            postDifferentValue(this)
        }
    }

    val rateInfoEvent: LiveData<Event<RateInfo>> = rateInfo.toEvent()


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

        val image: Int = 0,

        val title: String = "",
        val message: String = "",

        val positive: String = "",
        val negative: String = ""
    )
}