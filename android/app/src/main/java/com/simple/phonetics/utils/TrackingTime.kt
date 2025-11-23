package com.simple.phonetics.utils

import android.util.Log
import com.simple.phonetics.BuildConfig
import java.util.concurrent.ConcurrentHashMap

object TrackingTime {

    const val INIT_APP = "INIT_APP"

    private val map = ConcurrentHashMap<String, Long>()

    fun addTime(tag: String): Long = map[tag] ?: System.currentTimeMillis().apply {

        map[tag] = this
    }

    fun getTime(tag: String): Long = map[tag] ?: System.currentTimeMillis().apply {

        map[tag] = this
    }

    fun tracking(tag: String, vararg spaceWithTagName: String) {

        if (!BuildConfig.DEBUG) return

        val timeAdd = getTime(tag)

        Log.d("tuanha", "tracking: tag:${tag}  ${spaceWithTagName.map { "space_with_$it" to (timeAdd - getTime(it)) }}")
    }
}