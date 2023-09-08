package com.simple.phonetics.di

import com.simple.phonetics.data.api.Api
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

@JvmField
val apiModule = module {

    single {
        OkHttpClient
            .Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://github.com/")
            .addConverterFactory(JacksonConverterFactory.create())
            .client(get())
            .build()
    }

    single {
        get<Retrofit>().create(Api::class.java)
    }
}