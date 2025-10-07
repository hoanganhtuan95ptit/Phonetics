package com.simple.feature.campaign.data.api

import com.simple.feature.campaign.entities.Campaign
import com.simple.phonetics.BRANCH
import org.koin.core.context.GlobalContext
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

interface Api {

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/campaign/campaigns.json")
    suspend fun syncCampaign(@Path("branch") branch: String = BRANCH): List<Campaign>

    companion object {

        val api by lazy {
            GlobalContext.get().get<Retrofit>().create(Api::class.java)
        }
    }
}
