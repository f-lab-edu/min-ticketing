package com.flab.ticketing.performance.service

import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.dto.service.PerformanceStartEndDateResult
import com.flab.ticketing.performance.repository.reader.PerformanceReader
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired

class PerformanceServiceCacheTest : IntegrationTest() {

    @MockkBean
    private lateinit var performanceReader: PerformanceReader

    @Autowired
    private lateinit var performanceService: PerformanceService


    init {
        given("search 메서드가 Cache가 적용되었을 때") {
            val performances =
                PerformanceTestDataGenerator.createPerformanceGroupbyRegion(performanceCount = 5)

            every { performanceReader.findPerformanceEntityByCursor(any()) } returns performances
            every { performanceReader.findPerformanceStartAndEndDate(any()) } returns performances.map { performance ->
                PerformanceStartEndDateResult(
                    performance.id,
                    performance.performanceDateTime.minOf { it.showTime },
                    performance.performanceDateTime.maxOf { it.showTime })
            }

            `when`("동일한 파라미터로 2번 이상 검색시") {

                val cursorDto = CursorInfoDto(limit = 5)

                val searchResult1 = performanceService.search(cursorDto)
                val searchResult2 = performanceService.search(cursorDto)

                then("한번은 캐싱되어 호출된다.") {
                    verify(exactly = 1) { performanceReader.findPerformanceEntityByCursor(any()) }
                    verify(exactly = 1) { performanceReader.findPerformanceStartAndEndDate(any()) }
                    searchResult1 shouldContainExactly searchResult2
                }
            }
        }


    }

}