package com.phonetics.campaign.data.repositories

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.phonetics.campaign.data.api.Api
import com.phonetics.campaign.entities.Campaign
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.BRANCH
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class CampaignRepository {

    private val list: MediatorLiveData<List<Campaign>> = MediatorLiveData()

    suspend fun getCampaignListAsync(): Flow<List<Campaign>> = channelFlow {

        if (list.value == null) kotlin.runCatching {

            list.postDifferentValue(Api.api.syncCampaign(BRANCH))
        }

        list.asFlow().launchCollect(this) {
            trySend(it)
        }

        awaitClose {

        }
    }

    companion object{

        val instance by lazy {
            CampaignRepository()
        }
    }
}