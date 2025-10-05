package com.simple.okhttp.cache

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.databind.JsonNode
import com.simple.core.utils.extentions.toObject
import com.simple.okhttp.cache.data.dao.NetworkProvider
import com.simple.okhttp.cache.entities.Header
import com.simple.okhttp.cache.entities.NetworkEntity
import com.simple.phonetics.BuildConfig
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.security.MessageDigest

private val timeSession = System.currentTimeMillis()

private val inFlightRequests = HashMap<String, Object>()

class CacheInterceptor(context: Context) : Interceptor {

    private val networkDao by lazy {
        NetworkProvider(context).networkDao
    }

    override fun intercept(chain: Interceptor.Chain): Response {


        val originalRequest = chain.request()

        // ⚠️ Bỏ qua cache nếu là upload
        if (isUploadRequest(originalRequest)) {

            return chain.proceedOrDefault(originalRequest)
        }


        val currentTime = System.currentTimeMillis()


        val timeCache = getRequestTimeCache(request = originalRequest, currentTime = currentTime)

        if (timeCache.isEmpty()) {

            return chain.proceedOrDefault(originalRequest)
        }


        val request = originalRequest.newBuilder()
            .removeHeader(Header.Name.HEADER_TIME_CACHE)
            .build()

        return getResponseWithLock(chain, request, timeCache, currentTime)
    }

    private fun isUploadRequest(request: Request): Boolean {

        val method = request.method.uppercase()
        val isWriteMethod = method in listOf("POST", "PUT", "PATCH")

        val contentType = request.body?.contentType()?.toString() ?: ""
        val isMultipart = contentType.startsWith("multipart/")

        return isWriteMethod && isMultipart
    }

    private fun generateCacheKey(request: Request): String = buildString {

        append(request.method)
        append('|')
        append(request.url.toString())
        append('|')
        append(getRequestHeader(request))
        append('|')
        append(getRequestBody(request))
    }.let { rawKey ->

        MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun getResponseWithLock(chain: Interceptor.Chain, request: Request, timeCache: List<Long>, currentTime: Long): Response {

        val key = generateCacheKey(request)

        val response: Response

        try {

            val lock = synchronized(inFlightRequests) {

                inFlightRequests[key] ?: Object().apply { inFlightRequests[key] = this }
            }

            synchronized(lock) {

                response = getResponse(chain = chain, request = request, key = key, timeCacheList = timeCache, currentTime = currentTime, lock = lock)
            }
        } finally {

            synchronized(inFlightRequests) {

                inFlightRequests.remove(key)
            }
        }

        return response
    }

    private fun getResponse(chain: Interceptor.Chain, request: Request, key: String, timeCacheList: List<Long>, currentTime: Long, lock: Object): Response {

        val cached: NetworkEntity? = runCatching {

            networkDao.getOrNullEntity(id = key)
        }.getOrElse {

            null
        }


        val timeCache = timeCacheList.filter { it >= 0L }.minOrNull() ?: 0L

        if (DEBUG) if (cached != null && cached.createdTime > currentTime - timeCache) {

            Log.d(TAG, "get data from cache ==> lock:${System.identityHashCode(lock)} key:$key url:${request.url} request_body:${getRequestBody(request)}")
        }

        if (cached != null && cached.createdTime > currentTime - timeCache) {

            return buildCachedResponse(request, cached)
        }


        if (DEBUG) {

            Log.d(TAG, "get data from call api ==> lock:${System.identityHashCode(lock)} key:$key url:${request.url} request_body:${getRequestBody(request)}")
        }

        val response = chain.proceedOrDefault(request)


        if (DEBUG) if (response.code != 200) if (cached != null && timeCacheList.contains(Header.CachePolicy.USE_CACHE_WHEN_ERROR)) {

            Log.d(TAG, "get data from cache when call api error ==> lock:${System.identityHashCode(lock)} key:$key error:${response.code} url:${request.url} request_body:${getRequestBody(request)}")
        }

        if (response.code != 200) return if (cached != null && timeCacheList.contains(Header.CachePolicy.USE_CACHE_WHEN_ERROR)) {

            buildCachedResponse(request, cached)
        } else {

            response
        }


        val responseBody = response.body?.string() ?: ""

        val networkEntity = NetworkEntity(
            id = key,
            code = response.code,
            body = responseBody,
            message = response.message,
            createdTime = currentTime
        )

        networkDao.insertEntity(networkEntity)


        if (DEBUG) {

            Log.d(TAG, "insert data to cache ==> lock:${System.identityHashCode(lock)} key:$key url:${request.url} response_body:${responseBody.toObject<JsonNode>()} ")
        }

        return response.newBuilder()
            .body(responseBody.toResponseBody(response.body?.contentType()))
            .build()
    }

    private fun getRequestBody(request: Request): String {

        var bodyString = ""

        if (request.method != "GET") {
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            bodyString = buffer.readUtf8()
        }

        return bodyString
    }

    private fun getRequestHeader(request: Request): String {

        val importantHeaders = listOf("Content-Type")

        val headerPart = importantHeaders.joinToString("&") { headerName ->
            "$headerName=${request.header(headerName) ?: ""}"
        }

        return headerPart
    }

    private fun getRequestTimeCache(request: Request, currentTime: Long): List<Long> {

        val timeList = request.header(Header.Name.HEADER_TIME_CACHE)?.split(",")?.mapNotNull {

            when (val time = it.toLongOrNull()) {

                Header.CachePolicy.TIME_CACHE_BY_SESSION -> {
                    currentTime - timeSession
                }

                Header.CachePolicy.TIME_CACHE_BY_FOREVER -> {
                    currentTime
                }

                else -> {
                    time
                }
            }
        }

        return timeList.orEmpty()
    }

    private fun buildCachedResponse(request: Request, cached: NetworkEntity): Response {

        return Response.Builder()
            .request(request)
            .code(cached.code)
            .message(cached.message)
            .protocol(Protocol.HTTP_1_1)
            .body(cached.body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun Interceptor.Chain.proceedOrDefault(request: Request): Response = try {

        proceed(request)
    } catch (e: Exception) {

        Response.Builder()
            .request(request)
            .code(500)
            .message(e.message ?: e.javaClass.name)
            .protocol(Protocol.HTTP_1_1)
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
    }

    companion object {

        @Suppress("SimplifyBooleanWithConstants")
        private val DEBUG = BuildConfig.DEBUG && false
        private const val TAG = "tuanha->cache-interceptor"
    }
}