package com.phonetics.thanks.api

import com.simple.phonetics.BRANCH
import com.phonetics.thanks.Thank
import org.koin.core.context.GlobalContext
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

interface Api {

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/thanks/thanks.json")
    suspend fun syncCommunity(@Path("branch") branch: String = BRANCH): List<Thank>

    companion object {

        val api by lazy {
            GlobalContext.get().get<Retrofit>().create(Api::class.java)
        }
    }
}
