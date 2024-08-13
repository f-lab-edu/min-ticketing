package com.flab.ticketing.common

import com.flab.ticketing.common.entity.Region
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformancePlace
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PerformanceTestDataGenerator {

    private var regionCounter = 0
    private var seatCounter = 0
    private var performanceCounter = 0
    private var datetimeCounter = 0

    companion object {
        val INIT_PERFORMANCE_DATE = ZonedDateTime.of(LocalDateTime.of(2024, 1, 1, 0, 0), ZoneId.of("Asia/Seoul"))
    }

    fun createRegion(name: String): Region {
        return Region(generateUid("region"), name)
    }

    fun createPerformancePlace(region: Region, name: String, numSeats: Int): PerformancePlace {
        val place = PerformancePlace(region, name)
        repeat(numSeats) {
            val row = it / 10 + 1
            val column = it % 10 + 1
            place.addSeat(generateUid("seat"), row, column)
        }
        return place
    }

    fun createPerformance(place: PerformancePlace, name: String, numShowtimes: Int = 1): Performance {
        val performance = Performance(
            uid = generateUid("performance"),
            name = name,
            image = "https://example.com/image.jpg",
            description = "This is a test performance description",
            price = 50000,
            performancePlace = place
        )

        repeat(numShowtimes) {
            val showTime = INIT_PERFORMANCE_DATE.plusDays(it.toLong())
            performance.addDateTime(generateUid("datetime"), showTime)
        }

        return performance
    }

    fun generateUid(prefix: String): String {
        val counter = when (prefix) {
            "region" -> ++regionCounter
            "seat" -> ++seatCounter
            "performance" -> ++performanceCounter
            "datetime" -> ++datetimeCounter
            else -> throw IllegalArgumentException("Unknown prefix: $prefix")
        }
        return "$prefix${counter.toString().padStart(3, '0')}"
    }

}