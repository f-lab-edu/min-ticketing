package com.flab.ticketing.performance.integration

import com.flab.ticketing.testutils.NonCiIntegrationTest
import com.flab.ticketing.testutils.fixture.PerformanceFixture
import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import com.flab.ticketing.performance.repository.PerformanceSearchRepository
import com.flab.ticketing.performance.service.PerformanceService
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PerformanceSearchIntegrationTest(
    private val performanceSearchRepository: PerformanceSearchRepository,
    private val performanceService: PerformanceService
) : NonCiIntegrationTest() {

    init {
        given("공연 정보가 6개 이상 존재할 때") {

            val performances = PerformanceFixture.createPerformanceGroupbyRegion(
                performanceCount = 6
            )
            savePerformances(performances)


            `when`("사용자가 5개의 공연 정보를 조회할 시") {
                val uri = "/api/performances/search"
                val limit = 5

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", limit.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("5개의 공연 정보(1페이지)와 다음 검색을 위한 커서를 받을 수 있다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )
                    val expected = createSearchExpected(performances)
                    actual.cursor shouldNotBe null
                    actual.data.size shouldBe 5
                    actual.data.forEach { it shouldBeIn expected }
                }
            }
        }

        given("공연 정보가 5개 이하로 존재할 시") {

            val performances = PerformanceFixture.createPerformanceGroupbyRegion(
                performanceCount = 5
            )
            savePerformances(performances)

            `when`("사용자가 5개의 공연 정보를 조회할 시") {
                val uri = "/api/performances/search"
                val limit = 5

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", limit.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("cursor 정보는 NULL이고, 공연 정보가 존재하는 만큼 반환한다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpected(performances)

                    actual.cursor shouldNotBe null
                    actual.data.size shouldBe 5
                    actual.data shouldContainAll expected

                }
            }
        }

        given("공연 정보가 존재할 때 - 지역 검색") {

            val seoulRegionPerformances = PerformanceFixture.createPerformanceGroupbyRegion(
                regionName = "서울",
                performanceCount = 3
            )

            val gumiPerformanceCount = 3

            val gumiRegionPerformances = PerformanceFixture.createPerformanceGroupbyRegion(
                regionName = "구미",
                performanceCount = gumiPerformanceCount
            )

            savePerformances(seoulRegionPerformances)
            savePerformances(gumiRegionPerformances)

            `when`("특정 지역으로 공연을 검색할 시") {
                val uri = "/api/performances/search"
                val gumiRegionName = gumiRegionPerformances[0].performancePlace.region.name

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("region", gumiRegionName)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("필터링 된 공연 정보 리스트가 반환된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpected(gumiRegionPerformances)

                    actual.data.size shouldBe gumiPerformanceCount
                    actual.data shouldContainAll expected
                }
            }
        }
        given("공연 정보가 존재할 때 - 최저 금액 검색") {
            val region = PerformanceFixture.createRegion()
            val place = PerformanceFixture.createPerformancePlace(region)

            val performancePrices = listOf(2000, 3000, 4000)

            val performances = PerformanceFixture.createPerformancesPriceIn(
                place = place,
                priceIn = performancePrices
            )
            savePerformances(performances)

            `when`("최저으로 공연을 검색할 시") {
                val uri = "/api/performances/search"
                val minPrice = 3000

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("minPrice", minPrice.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("특정 금액 이상의 공연만 검색된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpected(performances.drop(1))

                    actual.data.size shouldBe expected.size
                    actual.data shouldContainAll expected
                }
            }
        }
        given("공연 정보가 존재할 때 - 최고 금액 검색") {

            val region = PerformanceFixture.createRegion()
            val place = PerformanceFixture.createPerformancePlace(region)

            val performancePrices = listOf(2000, 3000, 4000)

            val performances = PerformanceFixture.createPerformancesPriceIn(
                place = place,
                priceIn = performancePrices
            )
            savePerformances(performances)

            `when`("최고 금액으로 공연을 검색할 시") {
                val uri = "/api/performances/search"
                val maxPrice = 3000

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("maxPrice", maxPrice.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("특정 금액 이하의 공연만 검색된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpected(performances.dropLast(1))

                    actual.data.size shouldBe expected.size
                    actual.data shouldContainAll expected
                }
            }

        }

        given("공연 정보가 존재할 때 - 공연 날짜 검색") {

            val region = PerformanceFixture.createRegion()
            val place = PerformanceFixture.createPerformancePlace(region)

            val performance1DateTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )
            val performance2DateTime = ZonedDateTime.of(
                LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )

            val performances = PerformanceFixture.createPerformancesDatesIn(
                place = place,
                dateIn = listOf(performance1DateTime, performance2DateTime)
            )

            savePerformances(performances)


            `when`("특정 공연 날짜로 검색할 시") {
                val uri = "/api/performances/search"
                val searchDate = performance1DateTime

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("showTime", searchDate.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()



                then("특정 공연날짜가 포함된 공연만 반환한다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpected(listOf(performances[0]))

                    actual.data shouldContainAll expected
                }
            }
        }

        given("공연 정보가 존재할 때 - 공연 이름 검색") {
            val givenNames = listOf("멋진 공연", "예쁜 공연", "아주 멋진 공연")

            val region = PerformanceFixture.createRegion()
            val place = PerformanceFixture.createPerformancePlace(region)

            val performances = PerformanceFixture.createPerformancesInNames(
                place = place,
                nameIn = givenNames
            )
            savePerformances(performances)

            `when`("공연의 이름으로 검색할 시") {
                val uri = "/api/performances/search"
                val nameQuery = "멋진"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("q", nameQuery)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("검색 공연 이름이 포함된 공연만 조회된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpected(listOf(performances[0], performances[2]))

                    actual.data shouldContainAll expected

                }
            }
        }
        given("공연 정보가 존재할 때 - 멀티 조건 검색") {

            val region = PerformanceFixture.createRegion()
            val place = PerformanceFixture.createPerformancePlace(region)

            val performance1DateTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )
            val performance1Price = 50000
            val performance1Name = "공공 공연"

            val performance1 = PerformanceFixture.createPerformance(
                place = place,
                showTimeStartDateTime = performance1DateTime,
                price = performance1Price,
                name = performance1Name
            )

            val performance2 = PerformanceFixture.createPerformance(
                place = place,
                showTimeStartDateTime = performance1DateTime,
                price = 10000,
                name = performance1Name
            )

            val performance3 = PerformanceFixture.createPerformance(
                place = place,
                showTimeStartDateTime = ZonedDateTime.of(
                    LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                    ZoneId.of("Asia/Seoul")
                ),
                price = 15000
            )

            savePerformances(listOf(performance1, performance2, performance3))

            `when`("다양한 조건으로 공연을 검색할 시") {
                val uri = "/api/performances/search"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("showTime", performance1DateTime.toString())
                        .param("minPrice", (performance1Price - 1000).toString())
                        .param("maxPrice", (performance1Price + 5000).toString())
                        .param("region", region.name)
                        .param("q", "공공")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("모든 조건을 만족하는 공연 정보만이 조회된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpected(listOf(performance1))

                    actual.data shouldContainAll expected

                }
            }
        }

        given("다수의 공연 정보가 존재할 때") {

            val performances = PerformanceFixture.createPerformanceGroupbyRegion(
                performanceCount = 10
            )

            savePerformances(performances)

            val (cursor, prevPage) = performanceService.search(CursorInfoDto(limit = 5), PerformanceSearchConditions())
            `when`("특정 커서를 입력하여 공연 정보를 검색할 시") {
                val uri = "/api/performances/search"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("cursor", cursor)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()
                then("특정 커서 이상의 공연 정보를 조회한다.") {
                    val (_, nextPage) = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpected(performances)

                    // 이전 페이지 5개 + 다음 페이지 5개 -> 총 10개 조회
                    prevPage + nextPage shouldContainAll expected

                }
            }
        }
    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)
        withContext(Dispatchers.IO) {
            performanceSearchRepository.deleteAll()
        }
    }

    private fun savePerformances(performances: List<Performance>) {
        val saved = performances.map { PerformanceSearchSchema.of(it) }
        performanceSearchRepository.saveAll(saved)
    }

    private fun createSearchExpected(expectedPerformances: List<Performance>): List<PerformanceSummarySearchResult> {
        return expectedPerformances.map { PerformanceSummarySearchResult.of(it) }
    }
}