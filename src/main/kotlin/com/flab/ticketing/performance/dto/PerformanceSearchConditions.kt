package com.flab.ticketing.performance.dto

import java.time.LocalDateTime

data class PerformanceSearchConditions(
    val showTime: LocalDateTime,
    val minPrice: Int,
    val maxPrice: Int,
    val region: String
)