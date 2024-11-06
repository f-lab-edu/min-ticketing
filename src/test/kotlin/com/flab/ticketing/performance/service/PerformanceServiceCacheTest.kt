package com.flab.ticketing.performance.service

import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.config.CacheConfig
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.enums.CacheType
import com.flab.ticketing.performance.dto.service.PerformanceStartEndDateResult
import com.flab.ticketing.performance.repository.reader.PerformanceReader
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager


class PerformanceServiceCacheTest : IntegrationTest() {

    @MockkBean
    private lateinit var performanceReader: PerformanceReader

    @Autowired
    private lateinit var performanceService: PerformanceService

    @SpykBean(name = CacheConfig.LOCAL_CACHE_MANAGER_NAME)
    private lateinit var localCacheManager: CacheManager


    @SpykBean(name = CacheConfig.GLOBAL_CACHE_MANAGER_NAME)
    private lateinit var globalCacheManager: CacheManager

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


        given("search 메서드가 Cache가 적용되었을 때 - CompositeCacheManger 테스트") {
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

                performanceService.search(cursorDto)
                performanceService.search(cursorDto)

                then("두개의 CacheManager에서 모두 값을 조회한다.") {
                    verify(exactly = 2) { localCacheManager.getCache(CacheType.PRODUCT_CACHE_NAME) }
                    verify(exactly = 2) { globalCacheManager.getCache(CacheType.PRODUCT_CACHE_NAME) }
                }
            }

        }

        given("region list를 조회할 때") {
            // given
            val regions = MutableList(5) {
                PerformanceTestDataGenerator.createRegion("region$it")
            }

            every { performanceReader.getRegions() } returns regions

            `when`("두번이상 region list를 조회한다면") {
                val result1 = performanceService.getRegions()
                val result2 = performanceService.getRegions()

                then("두번째 부터는 캐싱된다.") {
                    verify(exactly = 1) { performanceReader.getRegions() }
                    result1 shouldContainExactly result2
                }
            }
        }


    }

}