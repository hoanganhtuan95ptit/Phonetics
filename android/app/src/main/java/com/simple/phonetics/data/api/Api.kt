package com.simple.phonetics.data.api

import com.simple.phonetics.BuildConfig
import com.simple.phonetics.entities.Event
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language
import okhttp3.ResponseBody

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

private val BRANCH: String
    get() = if (BuildConfig.DEBUG) "develop" else "main"

interface Api {

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/ipa/{language_code}/ipas.json")
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