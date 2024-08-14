package com.flab.ticketing.performance.dto

import java.time.ZonedDateTime

data class PerformanceSearchConditions(
    val showTime: ZonedDateTime? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val region: String? = null,
    val q:String? = null
)