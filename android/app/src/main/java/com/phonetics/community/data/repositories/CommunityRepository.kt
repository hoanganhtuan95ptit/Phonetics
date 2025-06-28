package com.phonetics.community.data.repositories

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.BRANCH
import com.phonetics.community.data.api.Api
import com.phonetics.community.entities.CommunityInvite
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class CommunityRepository {

    private val list: MediatorLiveData<List<CommunityInvite>> = MediatorLiveData()

    suspend fun getCommunitiesAsync(): Flow<List<CommunityInvite>> = channelFlow {

        if (list.value == null) kotlin.runCatching {

            list.postDifferentValue(Api.api.syncCommunity(BRANCH))
        }.getOrElse {

            Log.d("tuanha", "getCommunitiesAsync: ", it)
        }

        list.asFlow().launchCollect(this) {
            trySend(it)
        }

        awaitClose {

        }
    }

    companion object{

        val instance by lazy {
            CommunityRepository()
        }
    }
}