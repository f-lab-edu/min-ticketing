package com.flab.ticketing.testutils.persistence

import com.flab.ticketing.common.entity.Region
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformancePlace
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import com.flab.ticketing.testutils.generator.PerformanceTestDataGenerator
import java.time.ZonedDateTime

/**
 * performance 데이터를 영속화하는데 도움을 주는 클래스입니다.
 * @author minseok kim
 */
class PerformanceTestUtils(
    private val performanceRepository: PerformanceRepository,
    private val performancePlaceRepository: PerformancePlaceRepository,
    private val regionRepository: RegionRepository
) {

    /**
     * performance list를 저장합니다. performance의 region과 place는 모두 같다고 가정합니다.
     * @author minseok kim
     */
    fun savePerformances(performances: List<Performance>) {
        assert(performances.isNotEmpty())
        savePlaceAndRegion(performances[0].performancePlace, performances[0].performancePlace.region)
        performanceRepository.saveAll(performances)

    }

    /**
     * performance 객체를 생성하고 저장 후 반환합니다.
     * @author minseok kim
     */
    fun createAndSavePerformance(
        place: PerformancePlace = PerformanceTestDataGenerator.createPerformancePlace(),
        name: String = "공연",
        numShowtimes: Int = 1,
        showTimeStartDateTime: ZonedDateTime = PerformanceTestDataGenerator.INIT_PERFORMANCE_DATE,
        price: Int = 10000
    ): Performance {
        val performance = PerformanceTestDataGenerator.createPerformance(
            place, name, numShowtimes, showTimeStartDateTime, price
        )

        savePerformance(performance)

        return performance
    }

    /**
     * performance 객체와 region, place를 저장합니다.
     * @author minseok kim
     */
    fun savePerformance(performance: Performance) {
        savePlaceAndRegion(performance.performancePlace, performance.performancePlace.region)

        performanceRepository.save(performance)
    }


    private fun savePlaceAndRegion(place: PerformancePlace, region: Region) {
        regionRepository.save(region)
        performancePlaceRepository.save(place)
    }

    fun clearContext() {
        PerformanceTestDataGenerator.reset()
        performanceRepository.deleteAll()
        performancePlaceRepository.deleteAll()
        regionRepository.deleteAll()
    }

}