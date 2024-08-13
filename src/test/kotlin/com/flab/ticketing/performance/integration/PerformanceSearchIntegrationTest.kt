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

class PerformanceSearchIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var regionRepository: RegionRepository

    @Autowired
    private lateinit var placeRepository: PerformancePlaceRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    init {

        given("공연 정보가 6개 이상 존재할 때") {
            val performanceTestDataGenerator = PerformanceTestDataGenerator()

            val performances = performanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 6
            )

            regionRepository.save(performances[0].performancePlace.region)
            placeRepository.save(performances[0].performancePlace)

            performances.forEach {
                performanceRepository.save(it)
            }

            `when`("사용자가 5개의 공연 정보를 조회할 시") {
                val uri = "/api/performances?limit=5"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
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

                    val expected = performances.map {
                        PerformanceSearchResult(
                            it.uid,
                            it.image,
                            it.name,
                            it.performancePlace.region.name,
                            it.performanceDateTime.minOf { d -> d.showTime },
                            it.performanceDateTime.maxOf { d -> d.showTime }
                        )
                    }.asReversed().dropLast(1)

                    actual.cursor?.shouldBeEqual(performances[0].uid)
                    actual.data.size shouldBe 5
                    actual.data shouldContainExactly expected
                }
            }
        }

        given("공연 정보가 5개 이하로 존재할 시") {
            val performanceTestDataGenerator = PerformanceTestDataGenerator()

            val performances = performanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 5
            )

            regionRepository.save(performances[0].performancePlace.region)
            placeRepository.save(performances[0].performancePlace)

            performances.forEach {
                performanceRepository.save(it)
            }

            `when`("사용자가 5개의 공연 정보를 조회할 시") {
                val uri = "/api/performances?limit=5"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
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

                    val expected = performances.map {
                        PerformanceSearchResult(
                            it.uid,
                            it.image,
                            it.name,
                            it.performancePlace.region.name,
                            it.performanceDateTime.minOf { d -> d.showTime },
                            it.performanceDateTime.maxOf { d -> d.showTime }
                        )
                    }.asReversed()

                    actual.cursor shouldBe null
                    actual.data.size shouldBe 5
                    actual.data shouldContainExactly expected

                }
            }
        }

        given("공연 정보가 존재할 때 - 지역 검색") {
            val performanceTestDataGenerator = PerformanceTestDataGenerator()

            val seoulRegionPerformances = performanceTestDataGenerator.createPerformanceGroupbyRegion(
                regionName = "서울",
                performanceCount = 3
            )

            val gumiRegionName = "구미"
            val gumiPerformanceCount = 3

            val gumiRegionPerformances = performanceTestDataGenerator.createPerformanceGroupbyRegion(
                regionName = gumiRegionName,
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

                    val expected = gumiRegionPerformances.map {
                        PerformanceSearchResult(
                            it.uid,
                            it.image,
                            it.name,
                            it.performancePlace.region.name,
                            it.performanceDateTime.minOf { d -> d.showTime },
                            it.performanceDateTime.maxOf { d -> d.showTime }
                        )
                    }.asReversed()

                    actual.cursor shouldBe null
                    actual.data.size shouldBe gumiPerformanceCount
                    actual.data shouldContainExactly expected
                }
            }
        }
        given("공연 정보가 존재할 때 - 최저 금액 검색") {
            val performanceTestDataGenerator = PerformanceTestDataGenerator()

            val region = performanceTestDataGenerator.createRegion()
            val place = performanceTestDataGenerator.createPerformancePlace(region)

            val price2000Performance = performanceTestDataGenerator.createPerformance(place = place, price = 2000)
            val price3000Performance = performanceTestDataGenerator.createPerformance(place = place, price = 3000)
            val price4000Performance = performanceTestDataGenerator.createPerformance(place = place, price = 4000)


            regionRepository.save(region)
            placeRepository.save(place)
            performanceRepository.save(price2000Performance)
            performanceRepository.save(price3000Performance)
            performanceRepository.save(price4000Performance)


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

                    val expected = createSearchExpected(listOf(price4000Performance, price3000Performance))

                    actual.data.size shouldBe expected.size
                    actual.data shouldContainExactly expected
                }
            }
        }
    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)

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

    private fun createSearchExpected(performances: List<Performance>): List<PerformanceSearchResult> {
        return performances.map {
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