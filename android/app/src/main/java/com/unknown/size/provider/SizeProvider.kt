package com.unknown.size.provider

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow

interface SizeProvider {

    suspend fun provide(activity: FragmentActivity): Flow<Map<String, Int>>
}