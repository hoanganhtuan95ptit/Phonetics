package com.simple.okhttp.cache.entities

object Header {

    object Name {

        const val HEADER_TIME_CACHE = "X-Cache-Time"
    }

    object CachePolicy {

        /**
         * Cache chỉ có hiệu lực trong phiên làm việc hiện tại của app
         */
        const val TIME_CACHE_BY_SESSION = -1L

        /**
         * Cache có hiệu lực vĩnh viễn (cho đến khi xóa dữ liệu app)
         */
        const val TIME_CACHE_BY_FOREVER = -2L

        /**
         * Chỉ sử dụng cache khi request mạng bị lỗi
         */
        const val USE_CACHE_WHEN_ERROR = -3L
    }
}