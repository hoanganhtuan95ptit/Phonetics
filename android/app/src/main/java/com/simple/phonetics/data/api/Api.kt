package com.simple.phonetics.data.api

import com.simple.dao.entities.Ipa
import com.simple.phonetics.BRANCH
import com.simple.phonetics.entities.Event
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.WordTopic
import okhttp3.ResponseBody
import org.koin.core.context.GlobalContext
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface Api {

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/word/{language_code}/words.json")
    suspend fun syncWord(@Path("language_code") languageCode: String, @Path("branch") branch: String = BRANCH): List<WordTopic>

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/ipa/{language_code}/ipas-new.json")
    suspend fun syncIPA(@Path("language_code") languageCode: String, @Path("branch") branch: String = BRANCH): List<Ipa>

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/event/{language_code}/events.json")
    suspend fun syncEvent(@Path("language_code") languageCode: String, @Path("branch") branch: String = BRANCH): List<Event>

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/popular/{language_code}/populars.json")
    suspend fun syncPopular(@Path("language_code") languageCode: String, @Path("branch") branch: String = BRANCH): List<String>

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/configs.json")
    suspend fun syncConfig(@Path("branch") branch: String = BRANCH): Map<String, String>

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/translate/{language_code}/translates.json")
    suspend fun syncTranslate(@Path("language_code") languageCode: String, @Path("branch") branch: String = BRANCH): Map<String, String>

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/language/{language_code}/languages.json")
    suspend fun getLanguageSupport(@Path("language_code") languageCode: String, @Path("branch") branch: String = BRANCH): List<Language>

    @Streaming
    @GET
    suspend fun syncPhonetics(@Url url: String): ResponseBody
}

class ApiProvider{

    val api by lazy {
        GlobalContext.get().get<Retrofit>().create(Api::class.java)
    }
}