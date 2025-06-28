package com.phonetics.campaign.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import com.phonetics.campaign.data.repositories.CampaignRepository
import com.phonetics.campaign.entities.Campaign
import com.phonetics.campaign.ui.adapters.CampaignViewItem
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.toRich
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.getOrKey
import com.simple.phonetics.utils.exts.getOrTransparent
import kotlinx.coroutines.flow.firstOrNull

class CampaignHomeViewModel : BaseViewModel() {

    @VisibleForTesting
    val campaign: LiveData<Campaign> = mediatorLiveData {

        CampaignRepository.instance.getCampaignListAsync().firstOrNull()?.firstOrNull()?.let {
            postDifferentValue(it)
        }
    }

    val viewItemList: LiveData<List<ViewItem>> = combineSources(theme, translate, campaign) {

        val theme = theme.get()
        val translate = translate.get()
        val community = campaign.get()

        val viewItemList = arrayListOf<ViewItem>()

        CampaignViewItem(
            id = "CAMPAIGN",
            data = community,

            image = "",

            text = translate.getOrKey(community.title.orEmpty()).toRich(),
            message = translate.getOrKey(community.message.orEmpty()).toRich(),

            background = Background(
                backgroundColor = theme.getOrTransparent("colorPrimary"),
                cornerRadius = DP.DP_16
            )
        ).let {

            viewItemList.add(it)
        }

        postDifferentValueIfActive(viewItemList)
    }
}