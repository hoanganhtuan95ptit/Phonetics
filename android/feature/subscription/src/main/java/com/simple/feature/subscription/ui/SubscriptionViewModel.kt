package com.simple.feature.subscription.ui

import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
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
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.with
import com.simple.feature.subscription.R
import com.simple.feature.subscription.data.repositories.SubscriptionRepository
import com.simple.feature.subscription.entities.SubscriptionPlan
import com.simple.feature.subscription.ui.adapters.SubscriptionPlanViewItem
import com.simple.image.ImageRes
import com.simple.image.RichImage
import com.simple.phonetics.domain.usecase.GetConfigAsyncUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.colorDivider
import com.simple.phonetics.utils.exts.colorOnBackgroundVariant
import com.simple.phonetics.utils.exts.colorPrimaryVariant
import com.simple.phonetics.utils.exts.combineSourcesWithDiff
import com.simple.phonetics.utils.exts.get
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.exts.listenerSourcesWithDiff
import com.simple.phonetics.utils.exts.mutableSharedFlowWithDiff
import com.simple.phonetics.utils.exts.value
import com.simple.state.ResultState
import com.simple.state.doSuccess
import com.simple.state.isStart
import com.simple.state.isSuccess
import com.simple.state.toSuccess
import com.unknown.coroutines.handler
import com.unknown.theme.utils.exts.colorBackground
import com.unknown.theme.utils.exts.colorOnBackground
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SubscriptionViewModel : BaseViewModel() {

    val headerInfo: Flow<HeaderInfo> = combineSourcesWithDiff(themes, strings) {

        val themes = themes.value ?: return@combineSourcesWithDiff
        val strings = strings.value ?: return@combineSourcesWithDiff

        val info = HeaderInfo(
            back = ImageRes(R.drawable.ic_arrow_left_on_surface, themes.colorOnBackground),
            title = strings.getOrKey("title_subscription")
                .with(ForegroundColor(themes.colorOnBackground)),
            message = strings.getOrKey("message_select_subscription")
                .with(ForegroundColor(themes.colorOnBackgroundVariant)),
        )

        emit(info)
    }

    val config: Flow<Map<String, String>> = mutableSharedFlowWithDiff {

        GetConfigAsyncUseCase.install.execute().collect {

            emit(it)
        }
    }

    val subscriptionIdOld: Flow<String> = mutableSharedFlowWithDiff {

        emit("")

        SubscriptionRepository.getSubscriptionIdStateAsync().collect { state ->

            state.doSuccess {

                emit(it)
            }
        }
    }

    val subscriptionIdNew = combineSourcesWithDiff(subscriptionIdOld) {

        emit(subscriptionIdOld.get())
    }

    val subscriptionPlanListState: Flow<ResultState<List<SubscriptionPlan>>> = combineSourcesWithDiff(config) {

        val productIds = config.get()["subscription_ids"]?.split(",").orEmpty()
            .map { it.trim() }
            .takeIf { it.isNotEmpty() }

        SubscriptionRepository.getSubscriptionPlanStateAsync(productIds ?: listOf("premium_monthly_plan")).collect {

            emit(it)
        }
    }

    val subscriptionPlanViewItemList: Flow<List<ViewItem>> = listenerSourcesWithDiff(themes, strings, subscriptionIdNew, subscriptionPlanListState) {

        val themes = themes.get()
        val strings = strings.get()

        val subscriptionIdNew = subscriptionIdNew.value

        subscriptionPlanListState.get().toSuccess()?.data.orEmpty().map {

            val isSelected = it.id.equals(subscriptionIdNew, true)

            SubscriptionPlanViewItem(
                id = it.id,
                data = it,
                title = strings.getOrKey("title_${it.id}")
                    .with(Bold, ForegroundColor(themes.colorOnBackground)),
                description = strings.getOrKey("description_${it.id}")
                    .with(ForegroundColor(themes.colorOnBackgroundVariant)),
                price = it.price
                    .with(ForegroundColor(themes.colorOnBackgroundVariant)),

                background = Background(
                    cornerRadius = DP.DP_16,
                    strokeWidth = DP.DP_1,
                    strokeColor = if (isSelected) themes.colorPrimary else themes.colorDivider,
                    strokeDashGap = DP.DP_4,
                    strokeDashWidth = DP.DP_4,
                    backgroundColor = if (isSelected) themes.colorPrimaryVariant else Color.TRANSPARENT
                ),
            )
        }.let {

            emit(it)
        }
    }


    val confirmState = mutableSharedFlowWithDiff<ResultState<ResultInfo>> {

    }


    val confirmInfo: Flow<ConfirmInfo> = listenerSourcesWithDiff(themes, strings, subscriptionIdOld, subscriptionIdNew, subscriptionPlanListState, confirmState) {

        val themes = themes.value ?: return@listenerSourcesWithDiff
        val strings = strings.value ?: return@listenerSourcesWithDiff

        val confirmState = confirmState.value
        val subscriptionPlanListState = subscriptionPlanListState.value

        val subscriptionIdOld = subscriptionIdOld.value
        val subscriptionIdNew = subscriptionIdNew.value


        val isSelected = subscriptionIdOld != subscriptionIdNew

        val isClickable = isSelected && !confirmState.isSuccess() && subscriptionPlanListState.isSuccess()

        val info = ConfirmInfo(
            text = strings.getOrKey("action_confirm_change_subscription")
                .with(ForegroundColor(if (isSelected) themes.colorOnPrimary else themes.colorOnSurface)),
            isClickable = isClickable,
            isShowLoading = confirmState.isStart() || subscriptionPlanListState.isStart(),
            background = Background(
                cornerRadius = DP.DP_16,
                strokeWidth = DP.DP_1,
                strokeColor = if (isSelected) themes.colorPrimary else themes.colorDivider,
                backgroundColor = if (isSelected) themes.colorPrimary else Color.TRANSPARENT
            )
        )

        emit(info)
    }


    fun changeSubscriptionPlan() = viewModelScope.launch(handler + Dispatchers.IO) {

        val themes = themes.first()
        val strings = strings.first()

        confirmState.emit(ResultState.Start)

        val state = runCatching {

            SubscriptionRepository.subscription(subscriptionIdNew.get()).first()
        }.getOrElse {

            ResultState.Failed(it)
        }


        if (state is ResultState.Failed && state.cause is CancellationException) {

            confirmState.emit(state)
            return@launch
        }


        val list = arrayListOf<ViewItem>()

        val anim = if (state.isSuccess()) {
            R.raw.anim_subs_success
        } else {
            R.raw.anim_subs_error
        }

        ImageViewItem(
            id = "1",
            anim = anim,
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = DP.DP_100 + DP.DP_70
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_IMAGE", height = DP.DP_24))
        }


        val title = if (state.isSuccess()) {
           "title_subs_success"
        } else {
           "title_subs_error"
        }

        NoneTextViewItem(
            id = "2",
            text = strings.getOrKey(title)
                .with(Bold, ForegroundColor(themes.colorOnSurface)),
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
            list.add(SpaceViewItem("SPACE_TITLE", height = DP.DP_16))
        }


        val message = if (state.isSuccess()) {
            "message_subs_success"
        } else {
            "message_subs_error"
        }

        NoneTextViewItem(
            id = "3",
            text = strings.getOrKey(message)
                .with(ForegroundColor(themes.colorOnSurface)),
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            ),
            textSize = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            ),
            textStyle = TextStyle(
                textSize = 16f,
                textGravity = Gravity.CENTER
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_TITLE", height = DP.DP_40))
        }


        val action = if (state.isSuccess()) {
            "action_subs_success"
        } else {
            "action_subs_error"
        }

        val info = ResultInfo(

            background = Background(
                cornerRadius_TL = DP.DP_24,
                cornerRadius_TR = DP.DP_24,
                backgroundColor = themes.colorBackground
            ),
            viewItemList = list,

            positive = ButtonInfo(
                text = strings.getOrKey(action)
                    .with(ForegroundColor(themes.colorOnPrimary)),
                background = Background(
                    backgroundColor = themes.colorPrimary,
                    cornerRadius = DP.DP_16
                )
            )
        )

        confirmState.emit(ResultState.Success(info))
    }

    fun updateSubscriptionPlanSelected(data: SubscriptionPlan) {

        if (confirmState.value.isStart()) return

        subscriptionIdNew.tryEmit(data.id)
    }

    data class HeaderInfo(
        val back: RichImage,
        val title: RichText,
        val message: RichText,
    )

    data class ConfirmInfo(
        val text: RichText,

        val isClickable: Boolean,
        val isShowLoading: Boolean,

        val background: Background,
    )

    data class ResultInfo(
        val positive: ButtonInfo? = null,
        val background: Background,
        val viewItemList: List<ViewItem>? = null
    )
}
