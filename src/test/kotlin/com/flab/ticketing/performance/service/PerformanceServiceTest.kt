package com.flab.ticketing.performance.service

import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.order.service.ReservationService
import com.flab.ticketing.performance.dto.PerformanceDetailResponse
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.performance.repository.PerformanceRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class PerformanceServiceTest : UnitTest(){

    private val performanceRepository : PerformanceRepository = mockk()
    private val reservationService: ReservationService = mockk()
    private val performanceService : PerformanceService = PerformanceService(performanceRepository, reservationService)

    init {
        "Performance Detail 정보를 검색할 수 있다."{

            val performance = PerformanceTestDataGenerator.createPerformance(
                numShowtimes = 2
            )

            every { performanceRepository.findByUid(performance.uid) } returns performance
            every { reservationService.getReservationCount(any()) } returns 1

            val (actualUid, actualImage, actualTitle, actualRegion, actualPlace, actualPrice, actualDesc, actualDateInfo) = performanceService.searchDetail(
                performance.uid
            )

            val totalSeatSize = performance.performancePlace.seats.size.toLong()
            val expectedDateInfo = performance.performanceDateTime.map {
                PerformanceDetailResponse.DateInfo(
                    it.uid,
                    it.showTime.toLocalDateTime(),
                    totalSeatSize,
                    totalSeatSize - 1
                )
            }


            actualUid shouldBeEqual performance.uid
            actualImage shouldBeEqual performance.image
            actualTitle shouldBeEqual performance.name
            actualRegion shouldBeEqual performance.performancePlace.region.name
            actualPlace shouldBeEqual performance.performancePlace.name
            actualPrice shouldBeEqual performance.price
            actualDesc shouldBeEqual performance.description
            actualDateInfo shouldContainAll  expectedDateInfo

        }

        "performance Detail 정보 조회 실패시 NotFoundException을 throw한다."{

            every { performanceRepository.findByUid(any()) } returns null

            val e = shouldThrow<NotFoundException> {
                performanceService.searchDetail("uid")
            }

            e.info shouldBe PerformanceErrorInfos.PERFORMANCE_NOT_FOUND
        }

    }

}
