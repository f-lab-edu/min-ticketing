package com.flab.ticketing.performance.dto.service

import java.time.ZonedDateTime

data class PerformanceDateSummaryResult(
    val uid: String,
    val showTime: ZonedDateTime,
    val totalSeats: Long,
    val reservatedSeats: Long
)