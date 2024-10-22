package com.flab.ticketing.common.enums

import java.time.Duration

enum class CacheType(val cacheName: String, val localTtl: Duration, val globalTtl: Duration) {
    PRODUCT("product", Duration.ofMinutes(10), Duration.ofHours(1));


    companion object {
        fun getCacheNames() = values().map { it.cacheName }.toSet()
    }
}