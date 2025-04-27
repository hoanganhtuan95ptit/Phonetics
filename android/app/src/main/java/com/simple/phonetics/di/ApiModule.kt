package com.simple.phonetics.di

import com.simple.phonetics.data.api.Api
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


@JvmField
val apiModule = module {

    fun getUnsafeTrustManager(): X509TrustManager {

        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Không kiểm tra client
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Không kiểm tra server
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    fun getUnsafeSslSocketFactory(): SSLSocketFactory {

        val trustAllCerts = arrayOf<TrustManager>(getUnsafeTrustManager())

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return sslContext.socketFactory
    }

    single {

        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        OkHttpClient
            .Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .sslSocketFactory(getUnsafeSslSocketFactory(), getUnsafeTrustManager())
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
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