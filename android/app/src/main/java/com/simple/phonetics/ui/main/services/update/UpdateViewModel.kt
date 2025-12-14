package com.simple.phonetics.ui.main.services.update

import android.view.Gravity
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
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
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.Config.UPDATE_DEBUG
import com.simple.phonetics.Constants
import com.simple.phonetics.R
import com.simple.phonetics.domain.usecase.GetConfigAsyncUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.unknown.theme.utils.exts.colorBackground
import com.unknown.theme.utils.exts.colorOnPrimary
import com.unknown.theme.utils.exts.colorOnSurface
import com.unknown.theme.utils.exts.colorOnSurfaceVariant
import com.unknown.theme.utils.exts.colorPrimary

class UpdateViewModel(
    private val getConfigAsyncUseCase: GetConfigAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val config: LiveData<Map<String, String>> = mediatorLiveData {

        getConfigAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(theme, translate, config) {

        val theme = theme.get()
        val translate = translate.get()

        val config = config.get()

        val list = arrayListOf<ViewItem>()

        val newVersion = config[Constants.NEW_VERSION]?.toIntOrNull() ?: BuildConfig.VERSION_CODE

        if (!UPDATE_DEBUG) if (newVersion <= BuildConfig.VERSION_CODE || !translate.containsKey("update_title")) {

            postValue(list)
            return@combineSourcesWithDiff
        }

        ImageViewItem(
            id = "1",
            anim = R.raw.anim_update,
            size = Size(
                width = ViewGroup.LayoutParams.MATCH_PARENT,
                height = DP.DP_100 + DP.DP_100
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_IMAGE", height = DP.DP_24))
        }

        NoneTextViewItem(
            id = "2",
            text = translate["update_title"].orEmpty()
                .with(Bold, ForegroundColor(theme.colorOnSurface)),
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
            text = translate["update_message"].orEmpty()
                .with(ForegroundColor(theme.colorOnSurface)),
            textStyle = TextStyle(
                textSize = 16f,
                textGravity = Gravity.CENTER
            )
        ).let {

            list.add(it)
            list.add(SpaceViewItem("SPACE_TITLE", height = DP.DP_40))
        }

        postValue(list)
    }

    val updateInfo: LiveData<UpdateInfo> = combineSourcesWithDiff(theme, translate, viewItemList) {

        val theme = theme.value ?: return@combineSourcesWithDiff
        val translate = translate.value ?: return@combineSourcesWithDiff

        val viewItemList = viewItemList.value ?: return@combineSourcesWithDiff


        if (viewItemList.isEmpty()) {

            postValue(UpdateInfo(show = false))
            return@combineSourcesWithDiff
        }


        val info = UpdateInfo(
            show = true,

            viewItemList = viewItemList,

            positive = ButtonInfo(
                text = translate["update_action_positive"].orEmpty().with(ForegroundColor(theme.colorOnPrimary)),
                background = Background(
                    backgroundColor = theme.colorPrimary,
                    cornerRadius = DP.DP_16
                )
            ),
            negative = ButtonInfo(
                text = translate["update_action_negative"].orEmpty().with(ForegroundColor(theme.colorOnSurfaceVariant)),
                background = Background(
                    backgroundColor = theme.colorBackground,
                    strokeColor = theme.colorOnSurfaceVariant,
                    strokeWidth = DP.DP_1,
                    cornerRadius = DP.DP_16
                )
            ),
        )

        postValue(info)
    }

    data class UpdateInfo(
        val show: Boolean,

        val positive: ButtonInfo? = null,
        val negative: ButtonInfo? = null,

        val viewItemList: List<ViewItem>? = null,
    )
}