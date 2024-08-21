package com.flab.ticketing.performance.service

import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.performance.dto.response.PerformanceDateDetailResponse
import com.flab.ticketing.performance.dto.response.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.performance.dto.service.PerformanceDetailSearchResult
import com.flab.ticketing.performance.entity.PerformancePlace
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.performance.repository.PerformanceRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime

class PerformanceServiceTest : UnitTest() {

    private val performanceRepository: PerformanceRepository = mockk()
    private val performanceDateReader: PerformanceDateReader = mockk()
    private val performanceService: PerformanceService =
        PerformanceService(performanceRepository, performanceDateReader)

    init {
        "Performance Detail 정보를 검색할 수 있다." {

            val performance = PerformanceTestDataGenerator.createPerformance(
                numShowtimes = 2
            )

            val reservatedSeats = 2L

            val givenPerformanceInfo = PerformanceDetailSearchResult(
                uid = performance.uid,
                image = performance.image,
                title = performance.name,
                regionName = performance.performancePlace.region.name,
                placeName = performance.performancePlace.name,
                price = performance.price,
                description = performance.description
            )

            val givenDateInfos = performance.performanceDateTime.map {
                PerformanceDateSummaryResult(
                    it.uid,
                    it.showTime,
                    performance.performancePlace.seats.size.toLong(),
                    reservatedSeats
                )
            }

            every { performanceRepository.findByUid(performance.uid) } returns givenPerformanceInfo
            every { performanceDateReader.getDateInfo(performance.uid) } returns givenDateInfos

            val (actualUid, actualImage, actualTitle, actualRegion, actualPlace, actualPrice, actualDesc, actualDateInfo) = performanceService.searchDetail(
                performance.uid
            )

            val totalSeatSize = performance.performancePlace.seats.size.toLong()
            val expectedDateInfo = performance.performanceDateTime.map {
                PerformanceDetailResponse.DateInfo(
                    it.uid,
                    it.showTime.toLocalDateTime(),
                    totalSeatSize,
                    totalSeatSize - reservatedSeats
                )
            }


            actualUid shouldBeEqual performance.uid
            actualImage shouldBeEqual performance.image
            actualTitle shouldBeEqual performance.name
            actualRegion shouldBeEqual performance.performancePlace.region.name
            actualPlace shouldBeEqual performance.performancePlace.name
            actualPrice shouldBeEqual performance.price
            actualDesc shouldBeEqual performance.description
            actualDateInfo shouldContainAll expectedDateInfo

        }

        "performance Detail 정보 조회 실패시 NotFoundException을 throw한다." {

            every { performanceRepository.findByUid(any()) } returns null

            val e = shouldThrow<NotFoundException> {
                performanceService.searchDetail("uid")
            }

            e.info shouldBe PerformanceErrorInfos.PERFORMANCE_NOT_FOUND
        }

        "Performance Date의 좌석 정보를 조회할 수 있다." {
            val place = PerformancePlace(PerformanceTestDataGenerator.createRegion(), "장소")
            val seatDataList = listOf(
                1 to 1,
                1 to 2,
                2 to 1,
                2 to 2,
                2 to 3
            )

            seatDataList.forEachIndexed { index, (row, col) ->
                place.addSeat("seat$index", row, col)
            }


            val performance = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = ZonedDateTime.now().plusDays(1)
            )

            val performanceDateTime = performance.performanceDateTime[0]
            val reservatedUids = performance.performancePlace.seats.subList(1, 3).map { it.uid }

            every { performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performance.uid) } returns performance

            every {
                performanceDateReader.getReservatedSeatUids(
                    performance.performancePlace.id,
                    performanceDateTime.uid
                )
            } returns reservatedUids

            every {
                performanceDateReader.findByUid(performanceDateTime.uid)
            } returns performanceDateTime

            val actual =
                performanceService.getPerformanceSeatInfo(performance.uid, performanceDateTime.uid)

            val expectedSeats = listOf(
                listOf(
                    PerformanceDateDetailResponse.SeatInfo(
                        place.seats[0].uid,
                        place.seats[0].name,
                        false
                    ),
                    PerformanceDateDetailResponse.SeatInfo(
                        place.seats[1].uid,
                        place.seats[1].name,
                        true
                    )
                ),
                listOf(
                    PerformanceDateDetailResponse.SeatInfo(
                        place.seats[2].uid,
                        place.seats[2].name,
                        true
                    ),
                    PerformanceDateDetailResponse.SeatInfo(
                        place.seats[3].uid,
                        place.seats[3].name,
                        false
                    ),
                    PerformanceDateDetailResponse.SeatInfo(
                        place.seats[4].uid,
                        place.seats[4].name,
                        false
                    )
                )
            )



            actual.dateUid shouldBeEqual performanceDateTime.uid
            actual.pricePerSeat shouldBe performance.price

            for (i in 0..<actual.seats.size) {
                actual.seats[i] shouldContainExactly expectedSeats[i]
            }
        }

    }

}