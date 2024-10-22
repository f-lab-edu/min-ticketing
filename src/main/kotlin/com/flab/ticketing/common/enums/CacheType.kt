package com.flab.ticketing.common.enums

import java.time.Duration

enum class CacheType(val cacheName: String, val ttl: Duration) {
    PRODUCT("product", Duration.ofHours(1))
}