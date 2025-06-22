package com.simple.phonetics.ui.review

import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.adapters.ImageViewItem
import com.simple.coreapp.ui.adapters.SpaceViewItem
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
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
import com.simple.phonetics.utils.exts.getOrTransparent
import kotlinx.coroutines.flow.firstOrNull

class ReviewViewModel(
    private val getPhoneticsHistoryAsyncUseCase: GetPhoneticsHistoryAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val rate: LiveData<Rate> = MediatorLiveData()

    val historyList: LiveData<List<Sentence>> = mediatorLiveData {

        val list = getPhoneticsHistoryAsyncUseCase.execute(GetPhoneticsHistoryAsyncUseCase.Param(1)).firstOrNull()

        postValue(list)
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(theme, translate, rate, historyList) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val rate = rate.value ?: return@combineSources
        val historyList = historyList.value ?: return@combineSources

//        val isValidate = rate.date == 0 || (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - rate.date).absoluteValue >= 3
//
//        // nếu người dùng đã xác nhận mở rate
//        if (!RATE_DEBUG) if (rate.status == Rate.Status.OPEN_RATE.value || !isValidate || historyList.isEmpty()) {
//
//            postDifferentValue(emptyList())
//            return@combineSources
//        }


        val list = arrayListOf<ViewItem>()

        ImageViewItem(
            id = "1",
            anim = R.raw.anim_rate,
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = DP.DP_100 + DP.DP_70
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_IMAGE", height = DP.DP_24))
        }

        NoneTextViewItem(
            id = "2",
            text = translate["rate_title"].orEmpty()
                .with(Bold, ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            ),
            textSize = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            ),
            textStyle = TextStyle(
                textSize = 20f,
                textGravity = Gravity.CENTER
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_TITLE", height = DP.DP_24))
        }

        NoneTextViewItem(
            id = "3",
            text = translate["rate_message"].orEmpty()
                .with(ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
            textStyle = TextStyle(
                textSize = 16f,
                textGravity = Gravity.CENTER
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_TITLE", height = DP.DP_40))
        }

        postDifferentValue(list)
    }

    @VisibleForTesting
    val rateInfo: LiveData<RateInfo> = combineSources(theme, translate, viewItemList) {

        val theme = theme.value ?: return@combineSources
        val translate = translate.value ?: return@combineSources

        val viewItemList = viewItemList.value ?: return@combineSources


        if (viewItemList.isEmpty()) {

            postDifferentValue(RateInfo(show = false))
            return@combineSources
        }


        val info = RateInfo(
            show = true,

            viewItemList = viewItemList,

            positive = ButtonInfo(
                text = translate["rate_action_positive"].orEmpty().with(ForegroundColor(theme.getOrTransparent("colorOnPrimary"))),
                background = Background(
                    backgroundColor = theme.getOrTransparent("colorPrimary"),
                    cornerRadius = DP.DP_16
                )
            ),
            negative = ButtonInfo(
                text = translate["rate_action_negative"].orEmpty().with(ForegroundColor(theme.getOrTransparent("colorOnSurfaceVariant"))),
                background = Background(
                    backgroundColor = theme.getOrTransparent("colorBackground"),
                    strokeColor = theme.getOrTransparent("colorOnSurfaceVariant"),
                    strokeWidth = DP.DP_1,
                    cornerRadius = DP.DP_16
                )
            ),
        )

        postDifferentValue(info)
    }

    val rateInfoEvent: LiveData<Event<RateInfo>> = rateInfo.toEvent()


    fun updateRate(rate: Rate?) {

        if (this.rate.value != null) return
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

        val positive: ButtonInfo? = null,
        val negative: ButtonInfo? = null,

        val viewItemList: List<ViewItem>? = null,
    )
}