package com.simple.phonetics.utils.provider.color

import androidx.fragment.app.FragmentActivity
import com.google.auto.service.AutoService
import com.simple.coreapp.utils.extentions.getColorFromAttr
import com.unknown.color.provider.ColorProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

@AutoService(ColorProvider::class)
class DefaultColorProvider : ColorProvider {

    override suspend fun provide(activity: FragmentActivity): Flow<Map<String, Int>> = channelFlow {

        val map = hashMapOf<String, Int>()

        listOf(
            com.simple.coreapp.R.attr::class.java,
            com.simple.phonetics.R.attr::class.java,
        ).flatMap {

            it.fields.toList()
        }.forEach {

            if (it.name.startsWith("color", true)) kotlin.runCatching {

                map.put(it.name, activity.getColorFromAttr(it.getInt(null)))
            }
        }

        trySend(map)

        awaitClose {

        }
    }
}