package com.flab.ticketing.performance.dto.service

import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import java.time.LocalDate
import java.time.ZonedDateTime

data class PerformanceSummarySearchResult(
    val uid: String,
    val image: String,
    val title: String,
    val regionName: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?
) {
    constructor(
        uid: String,
        image: String,
        title: String,
        regionName: String,
        startDate: ZonedDateTime?,
        endDate: ZonedDateTime?
    ) : this(
        uid, image, title, regionName, startDate?.toLocalDate(), endDate?.toLocalDate()
    )

    companion object {

        fun of(performance: Performance): PerformanceSummarySearchResult {
            return PerformanceSummarySearchResult(
                performance.uid,
                performance.image,
                performance.name,
                performance.regionName,
                performance.performanceDateTime.minOf { it.showTime },
                performance.performanceDateTime.maxOf { it.showTime },
            )

        }

        fun of(performance: PerformanceSearchSchema): PerformanceSummarySearchResult {
            return PerformanceSummarySearchResult(
                performance.id,
                performance.image,
                performance.title,
                performance.region,
                performance.showTimes.minOf { it },
                performance.showTimes.maxOf { it }
            )
        }
    }

}