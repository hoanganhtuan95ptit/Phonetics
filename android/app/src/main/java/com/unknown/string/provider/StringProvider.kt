package com.unknown.string.provider

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow

interface StringProvider {

    suspend fun provide(activity: FragmentActivity): Flow<Map<String, String>>
}