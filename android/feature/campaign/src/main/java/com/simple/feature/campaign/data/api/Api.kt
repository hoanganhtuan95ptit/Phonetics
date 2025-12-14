package com.simple.feature.campaign.data.api

import com.simple.feature.campaign.entities.Campaign
import com.simple.okhttp.cache.entities.Header
import com.simple.phonetics.BRANCH
import org.koin.core.context.GlobalContext
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface Api {

    @Headers("${Header.Name.HEADER_TIME_CACHE}: ${Header.CachePolicy.TIME_CACHE_6_DAY},${Header.CachePolicy.USE_CACHE_WHEN_ERROR}")
    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/campaign/campaigns.json")
    suspend fun syncCampaign(@Path("branch") branch: String = BRANCH): List<Campaign>

    companion object {

        val api by lazy {
            GlobalContext.get().get<Retrofit>().create(Api::class.java)
        }
    }
}
