package com.simple.feature.campaign.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.adapter.entities.ViewItem
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.feature.campaign.data.repositories.CampaignRepository
import com.simple.feature.campaign.entities.Campaign
import com.simple.feature.campaign.ui.adapters.CampaignViewItem
import com.simple.phonetics.ui.base.adapters.SizeViewItem
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.getOrKey
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorPrimary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class CampaignHomeViewModel : BaseViewModel() {

    @VisibleForTesting
    val campaign: LiveData<Campaign> = mediatorLiveData {

        CampaignRepository.instance.getCampaignListAsync().firstOrNull()?.firstOrNull()?.let {
            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(size, style, theme, translate, campaign) {

        val size = size.get()
        val style = style.get()
        val theme = theme.get()
        val translate = translate.get()

        val campaign = campaign.get()

        val viewItemList = arrayListOf<ViewItem>()

        val backgroundColor = if (campaign.backgroundColor != 0) {
            campaign.backgroundColor
        } else {
            theme.colorPrimary
        }


        val title = translate.getOrKey(campaign.title.orEmpty())
        val titleSpan = title
            .with(Bold, ForegroundColor(campaign.titleColor))

        val messageSpan = translate.getOrKey(campaign.message.orEmpty())
            .with(ForegroundColor(campaign.messageColor))


        if (!title.contains("title_", true)) CampaignViewItem(
            id = "CAMPAIGN",
            data = campaign,

            image = campaign.image.orEmpty(),

            text = titleSpan,

            message = messageSpan,

            background = Background(
                cornerRadius = DP.DP_16,
                backgroundColor = backgroundColor
            ),
        ).let {

            viewItemList.add(it)
        }

        viewItemList.forEach {

            if (it is SizeViewItem) it.measure(appSize = size, style = style)
        }

        postValueIfActive(viewItemList)
    }

    init {

        viewItemList.asFlow().map { it.isNotEmpty() }.distinctUntilChanged().launchCollect(viewModelScope) {

            logAnalytics("feature_campaign_show_$it")
        }
    }
}