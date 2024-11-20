package com.flab.ticketing.performance.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.testutils.generator.PerformanceTestDataGenerator
import com.flab.ticketing.testutils.UnitTest
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.order.repository.reader.CartReader
import com.flab.ticketing.order.repository.reader.ReservationReader
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.response.PerformanceDateDetailResponse
import com.flab.ticketing.performance.dto.response.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.response.RegionInfoResponse
import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.performance.dto.service.PerformanceSearchResult
import com.flab.ticketing.performance.dto.service.PerformanceStartEndDateResult
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformancePlace
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.performance.repository.reader.PerformanceReader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.time.ZonedDateTime

class PerformanceServiceTest : UnitTest() {

    private val performanceReader: PerformanceReader = mockk()
    private val reservationReader: ReservationReader = mockk()
    private val cartReader: CartReader = mockk()
    private val objectMapper = spyk<ObjectMapper>()
    private val performanceService: PerformanceService =
        PerformanceService(performanceReader, reservationReader, cartReader, objectMapper)

    init {
        "Performance Detail 정보를 검색할 수 있다." {

            val performance = PerformanceTestDataGenerator.createPerformance(
                numShowtimes = 2
            )

            val reservedSeats = 2L
            val cartedSeats = 3L


            val givenDateInfos = performance.performanceDateTime.map {
                PerformanceDateSummaryResult(
                    it.uid,
                    it.showTime,
                    performance.performancePlace.seats.size.toLong(),
                    reservedSeats,
                    cartedSeats
                )
            }

            every { performanceReader.findPerformanceDetailDto(performance.uid) } returns performance
            every { performanceReader.findDateSummaryDto(performance.id) } returns givenDateInfos

            val (actualUid, actualImage, actualTitle, actualRegion, actualPlace, actualPrice, actualDesc, actualDateInfo) = performanceService.searchDetail(
                performance.uid
            )

            val totalSeatSize = performance.performancePlace.seats.size.toLong()
            val expectedDateInfo = performance.performanceDateTime.map {
                PerformanceDetailResponse.DateInfo(
                    it.uid,
                    it.showTime.toLocalDateTime(),
                    totalSeatSize,
                    totalSeatSize - reservedSeats - cartedSeats
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

            every { performanceReader.findPerformanceDetailDto(any()) } throws NotFoundException(
                PerformanceErrorInfos.PERFORMANCE_NOT_FOUND
            )

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
            val reservedUids = performance.performancePlace.seats.subList(1, 3).map { it.uid }
            val cartSeatUids = performance.performancePlace.seats.subList(3, 4).map { it.uid }

            every { performanceReader.findPerformanceEntityByUidJoinWithPlace(performance.uid) } returns performance
            every {
                reservationReader.findReserveUidInPlace(
                    performance.performancePlace.id,
                    performanceDateTime.uid
                )
            } returns reservedUids

            every {
                performanceReader.findDateEntityByUid(performance.uid, performanceDateTime.uid)
            } returns performanceDateTime
            every {
                cartReader.findSeatUidInPlace(performance.performancePlace.id, performanceDateTime.uid)
            } returns cartSeatUids


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
                        true
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

        "Performance의 목록을 조회하고, 각 Performance의 StartDate와 EndDate를 조회해 매핑 한 후 반환할 수 있다." {
            var performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 6
            )
            performances.forEachIndexed { idx, it -> (it as BaseEntity).setIdUsingReflection(idx.toLong()) }
            performances = performances.reversed()

            every { performanceReader.findPerformanceEntityByCursor(CursorInfoDto()) } returns performances
            every { performanceReader.findPerformanceStartAndEndDate(performances.map { it.id }) } returns createPerformanceStartAndEndDate(
                performances
            )
            val expected = createSearchExpectedOrderByIdDesc(performances)

            val actual = performanceService.search(CursorInfoDto())

            actual shouldContainExactly expected
        }

        "performance Search 검색 cursor가 null일 시 cursor를 String으로 변환하지 않고 null을 반환한다." {

            //given
            val givenReturnData = PerformanceSearchResult(null, listOf())

            every { performanceReader.search(any<PerformanceSearchConditions>(), any(), any()) } returns givenReturnData

            // when
            val (cursor, _) = performanceService.search(CursorInfoDto(), PerformanceSearchConditions())


            // then
            cursor shouldBe null
            verify(exactly = 0) { objectMapper.writeValueAsString(any<List<Any>>()) }
        }

        "Region 객체를 조회하여 RegionInfoResponse로 변환할 수 있다." {
            // given
            val regions = MutableList(5) {
                PerformanceTestDataGenerator.createRegion("region$it")
            }

            every { performanceReader.getRegions() } returns regions
            // when
            val actual = performanceService.getRegions()

            // then
            actual shouldContainAll regions.map { RegionInfoResponse(it.uid, it.name) }

        }
    }


    private fun createPerformanceStartAndEndDate(performances: List<Performance>): List<PerformanceStartEndDateResult> {
        return performances.map {
            PerformanceStartEndDateResult(
                it.id,
                it.performanceDateTime.minOf { it.showTime },
                it.performanceDateTime.maxOf { it.showTime })
        }
    }

    private fun createSearchExpectedOrderByIdDesc(performances: List<Performance>): List<PerformanceSummarySearchResult> {
        val sorted = performances.sortedBy { it.id }.asReversed()

        return sorted.map {
            PerformanceSummarySearchResult(
                it.uid,
                it.image,
                it.name,
                it.performancePlace.region.name,
                it.performanceDateTime.minOf { d -> d.showTime },
                it.performanceDateTime.maxOf { d -> d.showTime }
            )
        }
    }


}