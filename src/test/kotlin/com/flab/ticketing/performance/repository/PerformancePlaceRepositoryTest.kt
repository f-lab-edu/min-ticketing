package com.flab.ticketing.performance.repository

import com.flab.ticketing.testutils.PerformanceTestDataGenerator
import com.flab.ticketing.testutils.RepositoryTest
import com.flab.ticketing.performance.entity.PerformancePlace
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired

class PerformancePlaceRepositoryTest : RepositoryTest() {

    @Autowired
    private lateinit var placeRepository: PerformancePlaceRepository

    @Autowired
    private lateinit var regionRepository: RegionRepository

    @PersistenceContext
    private lateinit var em: EntityManager

    init {
        "Seat객체를 Place에 추가하고 생성하면 이름이 자동으로 설정되어 DB에 저장된다." {
            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformancePlace(region, "장소")

            place.addSeat("seat001", 1, 1)


            regionRepository.save(region)
            placeRepository.save(place)
            em.flush()
            em.clear()
            val actualPlace = placeRepository.findById(place.id)

            actualPlace.get().seats[0].name shouldBe "A1"

        }
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)
        placeRepository.deleteAll()
        regionRepository.deleteAll()
    }

}