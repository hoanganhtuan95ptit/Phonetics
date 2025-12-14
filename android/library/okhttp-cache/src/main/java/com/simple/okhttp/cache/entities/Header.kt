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
         * Cache có hiệu lực trong vòng 6 ngày
         */
        const val TIME_CACHE_1_DAY = 1 * 24 * 60 * 60 * 1000L

        /**
         * Cache có hiệu lực trong vòng 6 ngày
         */
        const val TIME_CACHE_6_DAY = 6 * 24 * 60 * 60 * 1000L

        /**
         * Chỉ sử dụng cache khi request mạng bị lỗi
         */
        const val USE_CACHE_WHEN_ERROR = -3L

    }
}