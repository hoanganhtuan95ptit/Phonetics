package com.simple.phonetics.data.api

import okhttp3.ResponseBody

import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface Api {

    @Streaming
    @GET
    suspend fun syncPhonetics(@Url url: String): ResponseBody
}