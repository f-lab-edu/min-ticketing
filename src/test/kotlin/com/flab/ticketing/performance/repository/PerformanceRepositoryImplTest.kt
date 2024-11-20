package com.flab.ticketing.performance.repository

import com.flab.ticketing.testutils.PerformanceTestDataGenerator
import com.flab.ticketing.testutils.RepositoryTest
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.performance.entity.Performance
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class PerformanceRepositoryImplTest(
    private val performanceRepository: PerformanceRepository
) : RepositoryTest() {

    @Autowired
    private lateinit var regionRepository: RegionRepository

    @Autowired
    private lateinit var placeRepository: PerformancePlaceRepository

    @Autowired
    private lateinit var performanceDateRepository: PerformanceDateRepository

    init {

        "performanceRepository 객체를 정상적으로 주입받을 수 있다." {
            performanceRepository shouldNotBe null
        }

        "Performance에 커서 정보를 넣어 커서 이상의 정보를 검색할 수 있다." {
            var performances =
                PerformanceTestDataGenerator.createPerformanceGroupbyRegion(performanceCount = 10)

            savePerformance(performances)

            performances = performances.asReversed()

            val limit = 5
            val startIdx = 3
            val actual = performanceRepository.search(
                CursorInfoDto(performances[startIdx].uid, limit)
            )

            val expected = List(limit) { idx -> performances[idx + startIdx].uid }

            actual.filterNotNull().map { it.uid } shouldContainExactly expected
        }

        "Performance를 UID로 검색할 수 있다." {
            val performance = PerformanceTestDataGenerator.createPerformance()

            savePerformance(listOf(performance))

            val findPerformance = performanceRepository.findByUid(
                performance.uid
            )!!

            findPerformance shouldBeEqual performance
        }

        "Performance를 UID로 검색할 시 UID에 해당하는 정보만 나온다." {
            val performances = List(5) {
                PerformanceTestDataGenerator.createPerformance()
            }

            savePerformance(performances)

            val actual = performanceRepository.findByUid(performances[2].uid)

            actual!!.uid shouldBe performances[2].uid

        }

        "Performance의 DateInfo를 검색할 수 있다." {
            val placeSeats = 10

            val showTimes = listOf(
                ZonedDateTime.of(
                    LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                    ZoneId.of("Asia/Seoul")
                ),
                ZonedDateTime.of(
                    LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                    ZoneId.of("Asia/Seoul")
                )
            )


            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = placeSeats),
                showTimes = showTimes
            )

            savePerformance(listOf(performance))

            val expected = performance.performanceDateTime.map {
                PerformanceDateSummaryResult(
                    uid = it.uid,
                    showTime = it.showTime.withZoneSameInstant(ZoneOffset.ofHours(9)),
                    totalSeats = placeSeats.toLong(),
                    reservedSeats = 0,
                    cartSeats = 0
                )
            }

            val actual = performanceDateRepository.getDateInfo(performance.id)

            actual shouldContainAll expected
        }


        "PerformanceDate를 UID로 검색할 시 해당 UID에 해당하는 Date정보만 조회한다." {
            val datePerPerformance = 5
            val performances = List(2) {
                PerformanceTestDataGenerator.createPerformance(numShowtimes = datePerPerformance)
            }

            savePerformance(performances)

            val actual = performanceDateRepository.getDateInfo(performances[0].id)
            actual.size shouldBe datePerPerformance
            actual.map { it.uid } shouldContainAll performances[0].performanceDateTime.map { it.uid }
        }

        "Performance를 UID로 FETCH JOIN해 검색할 수 있다." {
            val performances = List(2) {
                PerformanceTestDataGenerator.createPerformance()
            }

            savePerformance(performances)

            val actual =
                performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performances[0].uid)
            val expected = performances[0]

            actual!!.id shouldBe expected.id
            actual.performancePlace.id shouldBe expected.performancePlace.id
            actual.performancePlace.seats.map { it.uid } shouldContainAll expected.performancePlace.seats.map { it.uid }
        }

        "Performance를 List로 조회할 수 있다." {
            val givenPerformanceCnt = 5

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = givenPerformanceCnt
            )

            savePerformance(performances)

            val cursorSize = 5
            val actual = performanceRepository.search(CursorInfoDto(limit = cursorSize))

            val expected = performances.sortedBy { it.id }.asReversed()

            actual.size shouldBe cursorSize
            actual shouldContainExactly expected
        }

        "Performance를 Cursor로 포함하여 List로 조회할 수 있다." {
            var performances =
                PerformanceTestDataGenerator.createPerformanceGroupbyRegion(performanceCount = 10)

            savePerformance(performances)

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


    private fun savePerformance(performances: List<Performance>) {
        performances.forEach {
            regionRepository.save(it.performancePlace.region)
            placeRepository.save(it.performancePlace)
            performanceRepository.save(it)
        }
    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)
        PerformanceTestDataGenerator.reset()
    }
}