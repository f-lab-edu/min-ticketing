package com.flab.ticketing.performance.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class PerformanceDetailResponse(
    val uid: String,
    val image: String,
    val title: String,
    val region: String,
    val place: String,
    val price: Int,
    val description: String,
    val dateInfo: List<DateInfo>
) {
    data class DateInfo(
        val uid: String,
        val dateTime: LocalDateTime,
        val total: Long,
        val remaining: Long
    )

}