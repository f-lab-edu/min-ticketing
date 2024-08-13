package com.flab.ticketing.performance.repository

import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.RepositoryTest
import com.flab.ticketing.common.dto.CursorInfo
import com.flab.ticketing.performance.dto.PerformanceSearchConditions
import com.flab.ticketing.performance.entity.Performance
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired

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
            val performanceTestDataGenerator = PerformanceTestDataGenerator()

            val region = performanceTestDataGenerator.createRegion("서울")
            val place = performanceTestDataGenerator.createPerformancePlace(region, "공연장 1", 10)

            val performances = List(5) {
                performanceTestDataGenerator.createPerformance(place, "공연$it", 1)
            }

            regionRepository.save(region)
            placeRepository.save(place)

            performances.forEach {
                performanceRepository.save(it)
            }


            val actual = performanceRepository.search(PerformanceSearchConditions(), CursorInfo())

            actual.size shouldBe performances.size
            actual.filterNotNull().map { it.uid } shouldContainAll performances.map { it.uid }
            actual.filterNotNull().map { it.image } shouldContainAll performances.map { it.image }
            actual.filterNotNull().map { it.title } shouldContainAll performances.map { it.name }
            actual.filterNotNull().map { it.regionName } shouldContainAll List(5) { region.name }
        }

        "DB에 Performance List가 limit 이상의 갯수를 저장하고 있다면, 올바르게 limit개 만큼의 데이터를 갖고 올 수 있다." {
            val performanceTestDataGenerator = PerformanceTestDataGenerator()

            val performances = performanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 10,
                numShowtimes = 1,
                seatPerPlace = 1
            )

            regionRepository.save(performances[0].performancePlace.region)
            placeRepository.save(performances[0].performancePlace)

            performances.forEach {
                performanceRepository.save(it)
            }

            val limit = 5
            val actual = performanceRepository.search(PerformanceSearchConditions(), CursorInfo(limit = limit))

            actual.size shouldBe limit

        }

        "Performance를 Region UID로 필터링하여 조회할 수 있다." {
            val performanceTestDataGenerator = PerformanceTestDataGenerator()

            val seoulRegionPerformances = performanceTestDataGenerator.createPerformanceGroupbyRegion(
                regionName = "서울",
                performanceCount = 3
            )

            val gumiPerformanceCount = 3

            val gumiRegionPerformances = performanceTestDataGenerator.createPerformanceGroupbyRegion(
                regionName = "구미",
                performanceCount = gumiPerformanceCount
            )

            savePerformance(seoulRegionPerformances)
            savePerformance(gumiRegionPerformances)

            val regionUid = gumiRegionPerformances[0].performancePlace.region.uid
            val limit = 5
            val actual =
                performanceRepository.search(PerformanceSearchConditions(region = regionUid), CursorInfo(limit = 5))

            actual.size shouldBe gumiPerformanceCount
            actual.filterNotNull().map { it.uid } shouldContainAll gumiRegionPerformances.map { it.uid }

        }

        "Performance를 최소 금액으로 필터링하여 조회할 수 있다." {
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

            val minPrice = 3000
            val actual = performanceRepository.search(PerformanceSearchConditions(minPrice = minPrice), CursorInfo())

            actual.size shouldBe 2
        }
    }


    private fun savePerformance(performances: List<Performance>) {
        regionRepository.save(performances[0].performancePlace.region)
        placeRepository.save(performances[0].performancePlace)

        performances.forEach {
            performanceRepository.save(it)
        }
    }
}