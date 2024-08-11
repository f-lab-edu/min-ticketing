package com.flab.ticketing.performance.dto

import java.time.LocalDate

data class PerformanceDetailSearchResult(
    val image: String,
    val title: String,
    val place: String,
    val price: Int,
    val description: String,
    val dateInfo: List<DateInfo>
) {
    data class DateInfo(
        val date: LocalDate,
        val isSoldOut: Boolean
    )

}