package com.unknown.size.provider.def

import androidx.fragment.app.FragmentActivity
import com.google.auto.service.AutoService
import com.simple.coreapp.utils.ext.doOnHeightStatusAndHeightNavigationChange
import com.unknown.size.provider.SizeProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

@AutoService(SizeProvider::class)
class DefaultSizeProvider : SizeProvider {

    override suspend fun provide(activity: FragmentActivity): Flow<Map<String, Int>> = channelFlow {

        val map = HashMap<String, Int>()

        activity.doOnHeightStatusAndHeightNavigationChange { heightStatusBar, heightNavigationBar ->

            map["heightStatusBar"] = heightStatusBar
            map["heightNavigationBar"] = heightNavigationBar

            trySend(map)
        }

        awaitClose {
        }
    }
}