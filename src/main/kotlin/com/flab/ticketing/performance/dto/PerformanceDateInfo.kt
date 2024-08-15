package com.flab.ticketing.performance.dto

import java.time.ZonedDateTime

data class PerformanceDateInfo(
    val uid : String,
    val showTime: ZonedDateTime,
    val totalSeats: Long,
    val reservatedSeats: Long
)