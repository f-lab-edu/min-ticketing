package com.flab.ticketing.common

import com.flab.ticketing.common.entity.Region
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformancePlace
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

    fun createRegion(name: String = "지역"): Region {
        return Region(generateUid("region"), name)
    }

    fun createPerformancePlace(region: Region, name: String = " 공연장", numSeats: Int = 2): PerformancePlace {
        val place = PerformancePlace(region, name)
        repeat(numSeats) {
            val row = it / 10 + 1
            val column = it % 10 + 1
            place.addSeat(generateUid("seat"), row, column)
        }
        return place
    }

    fun createPerformance(
        place: PerformancePlace,
        name: String = "공연",
        numShowtimes: Int = 1,
        price: Int = 10000
    ): Performance {
        val performance = Performance(
            uid = generateUid("performance"),
            name = name,
            image = "https://example.com/image.jpg",
            description = "This is a test performance description",
            price = price,
            performancePlace = place
        )

        repeat(numShowtimes) {
            val showTime = INIT_PERFORMANCE_DATE.plusDays(it.toLong())
            performance.addDateTime(generateUid("datetime"), showTime)
        }

        return performance
    }

    fun createPerformanceGroupbyRegion(
        regionName: String = "서울",
        placeName: String = "장소",
        performanceCount: Int,
        numShowtimes: Int = 2,
        seatPerPlace: Int = 2
    ): List<Performance> {
        val region = createRegion(regionName)
        val place = createPerformancePlace(region, placeName, seatPerPlace)

        return List(performanceCount) {
            createPerformance(place, "공연$it", numShowtimes)
        }
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