package com.phonetics.thank.data.repositories

import android.util.Log
import com.phonetics.thank.entities.Thank
import com.phonetics.thank.data.api.Api
import com.phonetics.thank.data.dao.ThankProvider
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.BRANCH
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine

class CommunityRepository {

    suspend fun getCommunitiesAsync(): Flow<List<Thank>> = channelFlow {

        kotlin.runCatching {

            ThankProvider.pendingThankDao.insertOrUpdate(Api.api.syncCommunity(BRANCH))
        }.getOrElse {

            Log.d("tuanha", "getCommunitiesAsync: ", it)
        }

        combine(
            ThankProvider.sendThankDao.getListAsync(),
            ThankProvider.pendingThankDao.getListAsync()
        ) { ids, thanks ->

            thanks.filter { it.id !in ids }
        }.launchCollect(this) {

            trySend(it)
        }

        awaitClose {

        }
    }

    fun updateSendThank(deepLink: String) {
        ThankProvider.sendThankDao.insertOrUpdate(deepLink)
    }

    companion object {

        val instance by lazy {
            CommunityRepository()
        }
    }
}