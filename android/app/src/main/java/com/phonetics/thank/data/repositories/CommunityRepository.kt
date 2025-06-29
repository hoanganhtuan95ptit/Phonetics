package com.phonetics.thank.data.repositories

import com.phonetics.thank.data.api.Api
import com.phonetics.thank.data.dao.ThankProvider
import com.phonetics.thank.entities.Thank
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.BRANCH
import com.simple.phonetics.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine

class CommunityRepository {

    suspend fun getCommunitiesAsync(): Flow<List<Thank>> = channelFlow {

        kotlin.runCatching {

            ThankProvider.pendingThankDao.insertOrUpdate(Api.api.syncCommunity(BRANCH))
        }

        combine(
            ThankProvider.sendThankDao.getListAsync(),
            ThankProvider.pendingThankDao.getListAsync()
        ) { ids, thanks ->

            if (BuildConfig.DEBUG) {
                thanks
            } else {
                thanks.filter { it.id !in ids }
            }
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