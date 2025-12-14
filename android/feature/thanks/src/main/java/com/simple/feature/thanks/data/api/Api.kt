package com.simple.feature.thanks.data.api

import com.simple.feature.thanks.entities.Thank
import com.simple.okhttp.cache.entities.Header
import com.simple.phonetics.BRANCH
import org.koin.core.context.GlobalContext
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface Api {

    @Headers("${Header.Name.HEADER_TIME_CACHE}: ${Header.CachePolicy.TIME_CACHE_6_DAY},${Header.CachePolicy.USE_CACHE_WHEN_ERROR}")
    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/thank/thanks.json")
    suspend fun syncCommunity(@Path("branch") branch: String = BRANCH): List<Thank>

    companion object {

        val api by lazy {
            GlobalContext.get().get<Retrofit>().create(Api::class.java)
        }
    }
}
