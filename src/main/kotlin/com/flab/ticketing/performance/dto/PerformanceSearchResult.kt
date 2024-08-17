package com.flab.ticketing.performance.dto

import java.time.LocalDate
import java.time.ZonedDateTime

data class PerformanceSearchResult(
    val uid: String,
    val image: String,
    val title: String,
    val regionName: String,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    constructor(
        uid: String,
        image: String,
        title: String,
        regionName: String,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime
    ) : this(
        uid, image, title, regionName, startDate.toLocalDate(), endDate.toLocalDate()
    )

}