package com.flab.ticketing.performance.dto.service

import java.time.ZonedDateTime

data class PerformanceStartEndDateResult(
    val performanceId: Long,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime
)