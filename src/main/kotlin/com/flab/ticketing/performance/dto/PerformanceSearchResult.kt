package com.flab.ticketing.performance.dto

import java.time.LocalDate

data class PerformanceSearchResult(
    val image: String,
    val title: String,
    val regionName: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)