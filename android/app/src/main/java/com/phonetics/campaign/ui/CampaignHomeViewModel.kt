package com.phonetics.campaign.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.phonetics.campaign.data.repositories.CampaignRepository
import com.phonetics.campaign.entities.Campaign
import com.phonetics.campaign.ui.adapters.CampaignViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.with
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.exts.getOrTransparent
import kotlinx.coroutines.flow.firstOrNull

class CampaignHomeViewModel : BaseViewModel() {

    @VisibleForTesting
    val campaign: LiveData<Campaign> = mediatorLiveData {

        CampaignRepository.instance.getCampaignListAsync().firstOrNull()?.firstOrNull()?.let {
            postValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSourcesWithDiff(theme, translate, campaign) {

        val theme = theme.get()
        val translate = translate.get()

        val campaign = campaign.get()

        val viewItemList = arrayListOf<ViewItem>()

        val backgroundColor = if (campaign.backgroundColor != 0) {
            campaign.backgroundColor
        } else {
            theme.getOrTransparent("colorPrimary")
        }

        val title = translate.getOrKey(campaign.title.orEmpty())

        if (!title.contains("title_", true)) CampaignViewItem(
            id = "CAMPAIGN",
            data = campaign,

            image = campaign.image.orEmpty(),

            text = title
                .with(Bold, ForegroundColor(campaign.titleColor)),

            message = translate.getOrKey(campaign.message.orEmpty())
                .with(ForegroundColor(campaign.messageColor)),

            background = Background(
                cornerRadius = DP.DP_16,
                backgroundColor = backgroundColor
            )
        ).let {

            viewItemList.add(it)
        }

        postValueIfActive(viewItemList)
    }
}