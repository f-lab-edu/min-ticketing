package com.flab.ticketing.performance.dto

import java.time.LocalDateTime

data class PerformanceSearchConditions(
    val showTime: LocalDateTime? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val region: String? = null
)