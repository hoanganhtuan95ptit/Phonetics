package com.simple.phonetics.data.api

import com.simple.phonetics.BuildConfig
import com.simple.phonetics.entities.Language
import okhttp3.ResponseBody

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

private val BRANCH: String
    get() = if (BuildConfig.DEBUG) "develop" else "main"

interface Api {

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/translate/{language_code}/translates.json")
    suspend fun syncTranslate(@Path("language_code") languageCode: String, @Path("branch") branch: String = BRANCH): Map<String, String>

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/{branch}/configs/translate/{language_code}/languages.json")
    suspend fun getLanguageSupport(@Path("language_code") languageCode: String, @Path("branch") branch: String = BRANCH): List<Language>

    @Streaming
    @GET
    suspend fun syncPhonetics(@Url url: String): ResponseBody
}