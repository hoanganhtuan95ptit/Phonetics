package com.simple.phonetics.data.api

import okhttp3.ResponseBody

import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface Api {

    @Streaming
    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_UK.txt")
    suspend fun syncPhoneticsEnUk(): ResponseBody

    @Streaming
    @GET("https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_US.txt")
    suspend fun syncPhoneticsEnUs(): ResponseBody

    @Streaming
    @GET
    suspend fun syncPhonetics(@Url url: String): ResponseBody
}