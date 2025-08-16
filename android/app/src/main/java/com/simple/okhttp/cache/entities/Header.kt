package com.simple.okhttp.cache.entities

object Header {

    object Name {

        const val HEADER_TIME_CACHE = "HEADER_TIME_CACHE"
    }

    object Value {

        const val USE_CACHE_WHEN_ERROR = -3L
        const val TIME_CACHE_BY_SESSION = -1L
        const val TIME_CACHE_BY_FOREVER = -2L
    }
}