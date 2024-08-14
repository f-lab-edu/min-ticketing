package com.flab.ticketing.performance.integration

import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.dto.CursoredResponse
import com.flab.ticketing.performance.dto.PerformanceSearchResult
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PerformanceSearchIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var regionRepository: RegionRepository

    @Autowired
    private lateinit var placeRepository: PerformancePlaceRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    init {

        given("공연 정보가 6개 이상 존재할 때") {

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 6
            )

            savePerformance(performances)

            `when`("사용자가 5개의 공연 정보를 조회할 시") {
                val uri = "/api/performances"
                val limit = 5

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", limit.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("정렬조건은 최신 등록 순으로, 5개의 공연 정보(1페이지)와 다음 공연의 커서를 받을 수 있다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(performances.drop(1))

                    actual.cursor?.shouldBeEqual(performances[0].uid)
                    actual.data.size shouldBe 5
                    actual.data shouldContainExactly expected
                }
            }
        }

        given("공연 정보가 5개 이하로 존재할 시") {

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 5
            )
            savePerformance(performances)

            `when`("사용자가 5개의 공연 정보를 조회할 시") {
                val uri = "/api/performances"
                val limit = 5

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", limit.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("cursor 정보는 NULL이고, 공연 정보가 존재하는 만큼 반환한다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(performances)

                    actual.cursor shouldBe null
                    actual.data.size shouldBe 5
                    actual.data shouldContainExactly expected

                }
            }
        }

        given("공연 정보가 존재할 때 - 지역 검색") {

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

            `when`("특정 지역으로 공연을 검색할 시") {
                val uri = "/api/performances"
                val gumiRegionUid = gumiRegionPerformances[0].performancePlace.region.uid

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("region", gumiRegionUid)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("필터링 된 공연 정보 리스트가 반환된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(gumiRegionPerformances)

                    actual.cursor shouldBe null
                    actual.data.size shouldBe gumiPerformanceCount
                    actual.data shouldContainExactly expected
                }
            }
        }
        given("공연 정보가 존재할 때 - 최저 금액 검색") {
            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performancePrices = listOf(2000, 3000, 4000)

            val performances = PerformanceTestDataGenerator.createPerformancesPriceIn(
                place = place,
                priceIn = performancePrices
            )
            savePerformance(performances)

            `when`("최저으로 공연을 검색할 시") {
                val uri = "/api/performances"
                val minPrice = 3000

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("minPrice", minPrice.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("특정 금액 이상의 공연만 검색된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(performances.drop(1))

                    actual.data.size shouldBe expected.size
                    actual.data shouldContainExactly expected
                }
            }
        }
        given("공연 정보가 존재할 때 - 최고 금액 검색") {

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performancePrices = listOf(2000, 3000, 4000)

            val performances = PerformanceTestDataGenerator.createPerformancesPriceIn(
                place = place,
                priceIn = performancePrices
            )
            savePerformance(performances)

            `when`("최고 금액으로 공연을 검색할 시") {
                val uri = "/api/performances"
                val maxPrice = 3000

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("maxPrice", maxPrice.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("특정 금액 이하의 공연만 검색된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(performances.dropLast(1))

                    actual.data.size shouldBe expected.size
                    actual.data shouldContainExactly expected
                }
            }

        }

        given("공연 정보가 존재할 때 - 공연 날짜 검색") {

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performance1DateTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )
            val performance2DateTime = ZonedDateTime.of(
                LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )

            val performances = PerformanceTestDataGenerator.createPerformancesDatesIn(
                place = place,
                dateIn = listOf(performance1DateTime, performance2DateTime)
            )

            savePerformance(performances)


            `when`("특정 공연 날짜로 검색할 시") {
                val uri = "/api/performances"
                val searchDate = performance1DateTime

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("showTime", searchDate.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()



                then("특정 공연날짜가 포함된 공연만 반환한다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(listOf(performances[0]))

                    actual.cursor shouldBe null
                    actual.data shouldContainExactly expected
                }
            }
        }
        given("공연 정보가 존재할 때 - 멀티 조건 검색") {

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performance1DateTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )
            val performance1Price = 50000

            val performance1 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = performance1DateTime,
                price = performance1Price
            )

            val performance2 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = performance1DateTime,
                price = 10000
            )

            val performance3 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = ZonedDateTime.of(
                    LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                    ZoneId.of("Asia/Seoul")
                ),
                price = 15000
            )

            savePerformance(listOf(performance1, performance2, performance3))

            `when`("다양한 조건으로 공연을 검색할 시") {
                val uri = "/api/performances"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("showTime", performance1DateTime.toString())
                        .param("minPrice", (performance1Price - 1000).toString())
                        .param("maxPrice", (performance1Price + 5000).toString())
                        .param("region", region.uid)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("모든 조건을 만족하는 공연 정보만이 조회된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(listOf(performance1))

                    actual.cursor shouldBe null
                    actual.data shouldContainExactly expected

                }
            }
        }

        given("다수의 공연 정보가 존재할 때") {

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 10
            )

            savePerformance(performances)


            `when`("특정 커서를 입력하여 공연 정보를 검색할 시") {
                val uri = "/api/performances"

                // performance004, 003, 002를 조회, 001을 반환해야함.
                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "3")
                        .param("cursor", performances[3].uid)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()
                then("특정 커서 이상의 공연 정보를 조회한다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(performances)

                    actual.cursor!! shouldBeEqual expected[9].uid
                    actual.data shouldContainExactly expected.subList(6,9)

                }
            }
        }
    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)

        PerformanceTestDataGenerator.reset()
        performanceRepository.deleteAll()
        placeRepository.deleteAll()
        regionRepository.deleteAll()
    }

    private fun savePerformance(performances: List<Performance>) {
        regionRepository.save(performances[0].performancePlace.region)
        placeRepository.save(performances[0].performancePlace)

        performances.forEach {
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
}