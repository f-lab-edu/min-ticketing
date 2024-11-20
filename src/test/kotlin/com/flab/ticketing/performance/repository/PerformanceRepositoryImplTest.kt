package com.flab.ticketing.performance.repository

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.testutils.RepositoryTest
import com.flab.ticketing.testutils.fixture.PerformanceFixture
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZoneOffset

class PerformanceRepositoryImplTest(
    private val performanceRepository: PerformanceRepository
) : RepositoryTest() {


    @Autowired
    private lateinit var performanceDateRepository: PerformanceDateRepository

    init {

        "Performance에 커서 정보를 넣어 커서 이상의 정보를 검색할 수 있다." {
            var performances =
                PerformanceFixture.createPerformanceGroupbyRegion(performanceCount = 10)

            performancePersistenceUtils.savePerformances(performances)

            performances = performances.asReversed()

            val limit = 5
            val startIdx = 3
            val actual = performanceRepository.search(
                CursorInfoDto(performances[startIdx].uid, limit)
            )

            val expected = List(limit) { idx -> performances[idx + startIdx].uid }

            actual.map { it.uid } shouldContainExactly expected
        }

        "Performance를 UID로 검색할 수 있다." {
            val performance = performancePersistenceUtils.createAndSavePerformance()

            val findPerformance = performanceRepository.findByUid(
                performance.uid
            )!!

            findPerformance shouldBeEqual performance
        }

        "Performance를 UID로 검색할 시 UID에 해당하는 정보만 나온다." {
            val performances = PerformanceFixture.createPerformanceGroupbyRegion(
                performanceCount = 5
            )

            performancePersistenceUtils.savePerformances(performances)

            val actual = performanceRepository.findByUid(performances[2].uid)

            actual!!.uid shouldBe performances[2].uid

        }

        "Performance의 DateInfo를 검색할 수 있다." {
            val performance = performancePersistenceUtils.createAndSavePerformance(
                place = PerformanceFixture.createPerformancePlace(),
                numShowtimes = 2
            )
            val totalSeatCount = PerformanceFixture.INIT_PERFORMANCE_PLACE_SEAT_COUNT

            val expected = performance.performanceDateTime.map {
                PerformanceDateSummaryResult(
                    uid = it.uid,
                    showTime = it.showTime.withZoneSameInstant(ZoneOffset.ofHours(9)),
                    totalSeats = totalSeatCount.toLong(),
                    reservedSeats = 0,
                    cartSeats = 0
                )
            }

            val actual = performanceDateRepository.getDateInfo(performance.id)

            actual shouldContainAll expected
        }


        "PerformanceDate를 UID로 검색할 시 해당 UID에 해당하는 Date정보만 조회한다." {

            val performances = PerformanceFixture.createPerformanceGroupbyRegion(
                performanceCount = 2
            )
            val searchPerformance = performances[0]

            performancePersistenceUtils.savePerformances(performances)

            val actual = performanceDateRepository.getDateInfo(searchPerformance.id)
            actual.size shouldBe searchPerformance.performanceDateTime.size
            actual.map { it.uid } shouldContainAll searchPerformance.performanceDateTime.map { it.uid }
        }

        "Performance를 UID로 FETCH JOIN해 검색할 수 있다." {
            val performances = PerformanceFixture.createPerformanceGroupbyRegion(
                performanceCount = 2
            )

            performancePersistenceUtils.savePerformances(performances)

            val actual =
                performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performances[0].uid)
            val expected = performances[0]

            actual!!.id shouldBe expected.id
            actual.performancePlace.id shouldBe expected.performancePlace.id
            actual.performancePlace.seats.map { it.uid } shouldContainAll expected.performancePlace.seats.map { it.uid }
        }

        "Performance를 List로 조회할 수 있다." {
            val givenPerformanceCnt = 5

            val performances = PerformanceFixture.createPerformanceGroupbyRegion(
                performanceCount = givenPerformanceCnt
            )

            performancePersistenceUtils.savePerformances(performances)

            val cursorSize = 5
            val actual = performanceRepository.search(CursorInfoDto(limit = cursorSize))

            val expected = performances.sortedBy { it.id }.asReversed()

            actual.size shouldBe cursorSize
            actual shouldContainExactly expected
        }

        "Performance를 Cursor로 포함하여 List로 조회할 수 있다." {
            var performances =
                PerformanceFixture.createPerformanceGroupbyRegion(performanceCount = 10)

            performancePersistenceUtils.savePerformances(performances)

            performances = performances.sortedBy { it.id }.asReversed()

            val limit = 5
            val startIdx = 3
            val actual = performanceRepository.search(
                CursorInfoDto(performances[startIdx].uid, limit)
            )

            val expected = performances.subList(3, 8)

            actual shouldContainExactly expected
        }
    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)
        PerformanceFixture.reset()
        performancePersistenceUtils.clearContext()
    }
}