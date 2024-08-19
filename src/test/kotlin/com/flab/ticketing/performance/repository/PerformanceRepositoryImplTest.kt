package com.flab.ticketing.performance.repository

import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.RepositoryTest
import com.flab.ticketing.common.dto.CursorInfo
import com.flab.ticketing.performance.dto.PerformanceDateInfo
import com.flab.ticketing.performance.dto.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.PerformanceSearchResult
import com.flab.ticketing.performance.entity.Performance
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
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

    init {

        "performanceRepository 객체를 정상적으로 주입받을 수 있다." {
            performanceRepository shouldNotBe null
        }

        "Performance List를 조회할 수 있다." {
            val givenPerformanceCnt = 5

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = givenPerformanceCnt
            )

            savePerformance(performances)

            val cursorSize = 5
            val actual = performanceRepository.search(PerformanceSearchConditions(), CursorInfo(limit = cursorSize))

            val expected = createSearchExpectedOrderByIdDesc(performances)

            actual.size shouldBe cursorSize
            actual shouldContainExactly expected
        }

        "DB에 Performance List가 limit 이상의 갯수를 저장하고 있다면, 올바르게 limit개 만큼의 데이터를 갖고 올 수 있다." {

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 10,
                numShowtimes = 1,
                seatPerPlace = 1
            )
            savePerformance(performances)

            val limit = 5
            val actual = performanceRepository.search(PerformanceSearchConditions(), CursorInfo(limit = limit))

            actual.size shouldBe limit
        }

        "Performance를 Region UID로 필터링하여 조회할 수 있다." {

            val seoulRegionPerformances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                regionName = "서울",
                performanceCount = 3
            )

            val gumiPerformanceCount = 3

            val gumiRegionPerformances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                regionName = "구미",
                performanceCount = gumiPerformanceCount
            )

            savePerformance(seoulRegionPerformances)
            savePerformance(gumiRegionPerformances)

            val regionUid = gumiRegionPerformances[0].performancePlace.region.uid
            val actual = performanceRepository.search(
                PerformanceSearchConditions(region = regionUid),
                CursorInfo()
            )

            val expected = createSearchExpectedOrderByIdDesc(gumiRegionPerformances)

            actual.size shouldBe gumiPerformanceCount
            actual shouldContainExactly expected

        }

        "Performance를 최소 금액으로 필터링하여 조회할 수 있다." {
            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val givenPriceRange = listOf(2000, 3000, 4000)
            val performances = PerformanceTestDataGenerator.createPerformancesPriceIn(
                place = place,
                priceIn = givenPriceRange
            )

            savePerformance(performances)

            val minPrice = 3000
            val actual = performanceRepository.search(PerformanceSearchConditions(minPrice = minPrice), CursorInfo())

            actual.size shouldBe 2
            actual.filterNotNull().map { it.uid } shouldContainAll listOf(
                performances[1].uid,
                performances[2].uid
            )
        }

        "Performance를 최대 금액으로 필터링하여 조회할 수 있다." {

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val givenPriceRange = listOf(2000, 3000, 4000)
            val performances = PerformanceTestDataGenerator.createPerformancesPriceIn(
                place = place,
                priceIn = givenPriceRange
            )

            savePerformance(performances)

            val maxPrice = 3000
            val actual = performanceRepository.search(PerformanceSearchConditions(maxPrice = maxPrice), CursorInfo())

            actual.size shouldBe 2

            actual.filterNotNull().map { it.uid } shouldContainAll listOf(
                performances[0].uid,
                performances[1].uid
            )
        }

        "Performance를 공연 날짜로 필터링하여 조회할 수 있다." {

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            // 2024-1-1 10:00(Asia/Seoul) 공연 정보
            val performance1 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = ZonedDateTime.of(
                    LocalDateTime.of(2024, 1, 1, 10, 0, 0),
                    ZoneId.of("Asia/Seoul")
                ),
                numShowtimes = 2
            )

            // 2023-1-1 10:00(Asia/Seoul) 공연 정보
            val performance2 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = ZonedDateTime.of(
                    LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                    ZoneId.of("Asia/Seoul")
                ),
                numShowtimes = 2
            )

            savePerformance(listOf(performance1, performance2))

            // 2024-1-1 00:00시(Asia/Seoul)로 검색
            val searchShowTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZoneId.of("Asia/Seoul")
            )

            val actual = performanceRepository.search(
                PerformanceSearchConditions(showTime = searchShowTime),
                CursorInfo()
            )

            actual.size shouldBe 1
            actual[0]!!.uid shouldBe performance1.uid

        }

        "Performance를 공연 이름으로 필터링하여 조회할 수 있다." {
            val givenNames = listOf("예쁜 공연", "멋진 공연", "아주 멋진 공연", "공연 멋진", "공멋진연")

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performances = PerformanceTestDataGenerator.createPerformancesInNames(
                place = place,
                nameIn = givenNames
            )
            savePerformance(performances)

            val actual = performanceRepository.search(PerformanceSearchConditions(q = "멋진"), CursorInfo())

            val expected = createSearchExpectedOrderByIdDesc(performances.drop(1))

            actual.size shouldBe 4
            actual shouldContainExactly expected

        }


        "Performance를 모든 조건을 넣어 검색할 수 있다." {

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performance1DateTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )
            val performance1Price = 50000
            val performance1Name = "공공공연"

            val performance1 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = performance1DateTime,
                price = performance1Price,
                name = performance1Name
            )

            val performance2 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = performance1DateTime,
                price = 10000,
                name = performance1Name
            )

            val performance3 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = ZonedDateTime.of(
                    LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                    ZoneId.of("Asia/Seoul")
                ),
                price = performance1Price,
                name = performance1Name
            )

            val performance4 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = ZonedDateTime.of(
                    LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                    ZoneId.of("Asia/Seoul")
                ),
                price = 15000
            )

            savePerformance(listOf(performance1, performance2, performance3, performance4))

            val actual = performanceRepository.search(
                PerformanceSearchConditions(
                    performance1DateTime,
                    performance1Price - 1000,
                    performance1Price + 1000,
                    region.uid,
                    q = "공공"
                ), CursorInfo()
            )

            actual.size shouldBe 1
            actual[0]!!.uid shouldBe performance1.uid

        }

        "Performance에 커서 정보를 넣어 커서 이상의 정보를 검색할 수 있다." {
            var performances =
                PerformanceTestDataGenerator.createPerformanceGroupbyRegion(performanceCount = 10)

            savePerformance(performances)

            performances = performances.asReversed()

            val limit = 5
            val startIdx = 3
            val actual = performanceRepository.search(
                PerformanceSearchConditions(),
                CursorInfo(performances[startIdx].uid, limit)
            )

            val expected = List(limit) { idx -> performances[idx + startIdx].uid }

            actual.filterNotNull().map { it.uid } shouldContainExactly expected
        }

        "Performance를 UID로 검색할 수 있다." {
            val performance = PerformanceTestDataGenerator.createPerformance()

            savePerformance(listOf(performance))

            val (uid, image, title, regionName, placeName, price, description) = performanceRepository.findByUid(
                performance.uid
            )!!

            uid shouldBe performance.uid
            image shouldBe performance.image
            title shouldBe performance.name
            regionName shouldBe performance.performancePlace.region.name
            placeName shouldBe performance.performancePlace.name
            price shouldBe performance.price
            description shouldBe performance.description
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
                PerformanceDateInfo(
                    uid = it.uid,
                    showTime = it.showTime.withZoneSameInstant(ZoneOffset.ofHours(9)),
                    totalSeats = placeSeats.toLong(),
                    reservatedSeats = 0
                )
            }

            val actual = performanceRepository.getDateInfo(performance.uid)

            actual shouldContainAll expected
        }


        "PerformanceDate를 UID로 검색할 시 해당 UID에 해당하는 Date정보만 조회한다." {
            val datePerPerformance = 5
            val performances = List(2) {
                PerformanceTestDataGenerator.createPerformance(numShowtimes = datePerPerformance)
            }

            savePerformance(performances)

            val actual = performanceRepository.getDateInfo(performances[0].uid)
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
    }


    private fun savePerformance(performances: List<Performance>) {
        performances.forEach {
            regionRepository.save(it.performancePlace.region)
            placeRepository.save(it.performancePlace)
            performanceRepository.save(it)
        }
    }

    private fun createSearchExpectedOrderByIdDesc(performances: List<Performance>): List<PerformanceSearchResult> {
        val sorted = performances.sortedBy { it.id }.asReversed()

        return sorted.map {
            PerformanceSearchResult(
                it.uid,
                it.image,
                it.name,
                it.performancePlace.region.name,
                it.performanceDateTime.minOf { d -> d.showTime },
                it.performanceDateTime.maxOf { d -> d.showTime }
            )
        }
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)
        PerformanceTestDataGenerator.reset()
    }
}