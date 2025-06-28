package com.phonetics.thank.deeplinks

import android.content.ComponentCallbacks
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.asFlow
import com.phonetics.thank.ThankViewModel
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.Param
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
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.phonetics.ui.base.ConfirmViewModel
import com.simple.phonetics.ui.base.VerticalConfirmSheetFragment
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.exts.getOrTransparent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

@Deeplink("thank")
class ThankDeeplink : DeeplinkHandler {

    override suspend fun acceptDeeplink(deepLink: String): Boolean {
        return deepLink.startsWith("thank:", true)
    }


    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is FragmentActivity) {

            return false
        }


        val viewModel = componentCallbacks.viewModels<ThankViewModel>().value


        val theme = viewModel.theme.asFlow().first()
        val translate = viewModel.translate.asFlow().first()

        val thank = viewModel.thank.asFlow().firstOrNull().orEmpty()[deepLink] ?: return true


        val viewItemList = arrayListOf<ViewItem>()

        ImageViewItem(
            id = "1",
            image = thank.image,
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = DP.DP_100 + DP.DP_100
            )
        ).let {

            viewItemList.add(it)
            viewItemList.add(SpaceViewItem("SPACE_IMAGE", height = DP.DP_24))
        }

        NoneTextViewItem(
            id = "2",
            text = translate.getOrKey(thank.title.orEmpty())
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

            viewItemList.add(it)
            viewItemList.add(SpaceViewItem("SPACE_TITLE", height = DP.DP_24))
        }

        NoneTextViewItem(
            id = "3",
            text = translate.getOrKey(thank.message.orEmpty())
                .with(ForegroundColor(theme.getOrTransparent("colorOnSurface"))),
            textStyle = TextStyle(
                textSize = 16f,
                textGravity = Gravity.CENTER
            )
        ).let {

            viewItemList.add(it)
            viewItemList.add(SpaceViewItem("SPACE_TITLE", height = DP.DP_40))
        }


        val cancel = true

        val anchor = Background(
            cornerRadius = DP.DP_100,
            backgroundColor = theme.getOrTransparent("colorDivider"),
        )

        val background = Background(
            cornerRadius_TL = DP.DP_24,
            cornerRadius_TR = DP.DP_24,
            backgroundColor = theme.getOrTransparent("colorBackground")
        )

        val positive = ButtonInfo(
            text = translate.getOrKey(thank.positive.orEmpty())
                .with(ForegroundColor(theme.getOrTransparent("colorOnPrimary"))),
            background = Background(
                backgroundColor = theme.getOrTransparent("colorPrimary"),
                cornerRadius = DP.DP_16
            )
        )

        val negative = ButtonInfo(
            text = translate.getOrKey(thank.positive.orEmpty())
                .with(ForegroundColor(theme.getOrTransparent("colorOnSurfaceVariant"))),
            background = Background(
                backgroundColor = theme.getOrTransparent("colorBackground"),
                strokeColor = theme.getOrTransparent("colorOnSurfaceVariant"),
                strokeWidth = DP.DP_1,
                cornerRadius = DP.DP_16
            )
        )


        val id = UUID.randomUUID().toString()
        val keyRequest = "KEY_REQUEST_THANK"


        val confirmViewModel by componentCallbacks.viewModels<ConfirmViewModel>()

        confirmViewModel.updateInfo(
            id = id,
            keyRequest = keyRequest,

            viewItem = viewItemList,

            anchor = anchor,
            background = background,

            negative = negative,
            positive = positive,
        )

        listenerEvent(componentCallbacks.lifecycle, keyRequest) {

            val result = it.asObjectOrNull<Int>() ?: return@listenerEvent

            Log.d("tuanha", "navigation: $result")

            if (result == 0) sendDeeplink(thank.negativeDeeplink.orEmpty())
            if (result == 1) sendDeeplink(thank.positiveDeeplink.orEmpty())

            viewModel.sendThank(deepLink)
        }

        val fragment = VerticalConfirmSheetFragment()
        fragment.arguments = bundleOf(
            Param.ID to id,
            Param.CANCEL to cancel,
            Param.KEY_REQUEST to keyRequest,
        )
        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, "")

        return true
    }
}