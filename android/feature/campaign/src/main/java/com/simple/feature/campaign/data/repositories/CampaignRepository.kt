package com.simple.feature.campaign.data.repositories

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.feature.campaign.data.api.Api
import com.simple.feature.campaign.entities.Campaign
import com.simple.phonetics.BRANCH
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class CampaignRepository {

    private val list: MediatorLiveData<List<Campaign>> = MediatorLiveData()

    suspend fun getCampaignListAsync(): Flow<List<Campaign>> = channelFlow {

        if (list.value == null) runCatching {

            list.postValue(Api.api.syncCampaign(BRANCH))
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