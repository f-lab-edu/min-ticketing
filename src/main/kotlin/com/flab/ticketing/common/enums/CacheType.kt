package com.flab.ticketing.common.enums

import java.time.Duration


enum class CacheType(val cacheName: String, val localTtl: Duration, val globalTtl: Duration) {
    PRODUCT("product", Duration.ofMinutes(10), Duration.ofHours(1)),
    REGION("region", Duration.ofMinutes(30), Duration.ofHours(3));

    companion object {
        const val PRODUCT_CACHE_NAME = "product"
        const val REGION_CACHE_NAME = "region"
        fun getCacheNames() = values().map { it.cacheName }.toSet()
    }
}