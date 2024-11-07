package com.simple.phonetics.data.api

import okhttp3.ResponseBody

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface Api {

    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/main/configs/{language_code}/translates.json")
    suspend fun syncTranslate(@Path("language_code") languageCode: String): Map<String, String>

    @Streaming
    @GET
    suspend fun syncPhonetics(@Url url: String): ResponseBody
}