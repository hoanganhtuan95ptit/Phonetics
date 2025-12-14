package com.simple.feature.thanks.data.repositories

import com.simple.feature.thanks.data.api.Api
import com.simple.feature.thanks.data.dao.ThankProvider
import com.simple.feature.thanks.entities.Thank
import com.simple.phonetics.BRANCH
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine

class CommunityRepository {

    suspend fun getCommunitiesAsync(): Flow<List<Thank>> = channelFlow {

        runCatching {

            ThankProvider.pendingThankDao.insertOrUpdate(Api.Companion.api.syncCommunity(BRANCH))
        }

        combine(
            ThankProvider.sendThankDao.getListAsync(),
            ThankProvider.pendingThankDao.getListAsync()
        ) { ids, thanks ->

            thanks.filter { it.id !in ids || it.id.orEmpty().contains("ads") }
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