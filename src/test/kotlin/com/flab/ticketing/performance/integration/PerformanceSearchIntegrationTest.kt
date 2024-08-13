package com.flab.ticketing.performance.integration

import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.dto.CursoredResponse
import com.flab.ticketing.performance.dto.PerformanceSearchResult
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
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
        

    }

}