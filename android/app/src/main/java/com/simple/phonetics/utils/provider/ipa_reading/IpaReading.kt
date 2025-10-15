package com.simple.phonetics.utils.provider.ipa_reading

import com.simple.autobind.AutoBind
import com.simple.dao.entities.Ipa
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

interface IpaReading {

    fun order() = 0

    fun reading(ipa: Ipa, phoneticCode: String): Flow<ResultState<String>>

    companion object {

        val install by lazy {
            AutoBind.loadAsync(IpaReading::class.java, false)
        }
    }
}