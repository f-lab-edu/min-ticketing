package com.flab.ticketing.testutils

import com.flab.ticketing.common.entity.Region
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformancePlace
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

object PerformanceTestDataGenerator {

    private var regionCounter = 0
    private var seatCounter = 0
    private var performanceCounter = 0
    private var datetimeCounter = 0
    private val INIT_PERFORMANCE_DATE = ZonedDateTime.of(LocalDateTime.of(2024, 1, 1, 0, 0), ZoneId.of("Asia/Seoul"))

    fun createRegion(name: String = "지역"): Region {
        return Region(generateUid("region"), name)
    }

    fun createPerformancePlace(
        region: Region = createRegion(),
        name: String = " 공연장",
        numSeats: Int = 2
    ): PerformancePlace {
        val place = PerformancePlace(region, name)
        repeat(numSeats) {
            val row = it / 10 + 1
            val column = it % 10 + 1
            place.addSeat(generateUid("seat"), row, column)
        }
        return place
    }

    fun createPerformance(
        place: PerformancePlace = createPerformancePlace(),
        name: String = "공연",
        showTimes: List<ZonedDateTime>,
        price: Int = 10000,
        image: String = "http://image.com/image/1",
        description: String = "공연 설명"
    ): Performance {

        val performance = Performance(
            uid = generateUid("performance"),
            name = name,
            image = image,
            description = description,
            price = price,
            performancePlace = place,
            placeName = place.name,
            regionName = place.region.name
        )

        for (showTime in showTimes) {
            performance.addDateTime(generateUid("datetime"), showTime)
        }

        return performance
    }

    fun createPerformance(
        place: PerformancePlace = createPerformancePlace(),
        name: String = "공연",
        numShowtimes: Int = 1,
        showTimeStartDateTime: ZonedDateTime = INIT_PERFORMANCE_DATE,
        price: Int = 10000
    ): Performance {
        val performance = Performance(
            uid = generateUid("performance"),
            name = name,
            image = "https://example.com/image.jpg",
            description = "This is a test performance description",
            price = price,
            performancePlace = place,
            placeName = place.name,
            regionName = place.region.name
        )

        repeat(numShowtimes) {
            val showTime = showTimeStartDateTime.plusDays(it.toLong())
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

    /**
     * Performance를 가격별로 생성하는 메소드로, 순서는 PriceIn과 동일합니다.
     */
    fun createPerformancesPriceIn(
        place: PerformancePlace = createPerformancePlace(),
        priceIn: List<Int>,
        numShowtimes: Int = 2,
        showTimeStartDateTime: ZonedDateTime = INIT_PERFORMANCE_DATE
    ): MutableList<Performance> {
        val result = mutableListOf<Performance>()
        for (price in priceIn) {
            result.add(
                createPerformance(
                    place = place,
                    numShowtimes = numShowtimes,
                    showTimeStartDateTime = showTimeStartDateTime,
                    price = price
                )
            )
        }

        return result
    }

    /**
     * Performance를 가격별로 생성하는 메소드로, 순서는 DateIn과 동일합니다.
     */
    fun createPerformancesDatesIn(
        place: PerformancePlace = createPerformancePlace(),
        dateIn: List<ZonedDateTime>,
        numShowtimes: Int = 2,
        price: Int = 10000
    ): MutableList<Performance> {
        val result = mutableListOf<Performance>()
        for (date in dateIn) {
            result.add(
                createPerformance(
                    place = place,
                    numShowtimes = numShowtimes,
                    showTimeStartDateTime = date,
                    price = price
                )
            )
        }

        return result
    }

    /**
     * Performance를 이름별로 생성하는 메소드로, 순서는 nameIn과 동일합니다.
     */
    fun createPerformancesInNames(
        place: PerformancePlace = createPerformancePlace(),
        nameIn: List<String>,
        numShowtimes: Int = 2,
        price: Int = 10000
    ): MutableList<Performance> {
        val result = mutableListOf<Performance>()
        for (name in nameIn) {
            result.add(
                createPerformance(
                    place = place,
                    numShowtimes = numShowtimes,
                    name = name,
                    price = price
                )
            )

        }
        return result

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


    fun reset() {
        regionCounter = 0
        seatCounter = 0
        performanceCounter = 0
        datetimeCounter = 0

    }

}