package com.flab.ticketing.performance.repository

import com.flab.ticketing.common.RepositoryTest
import com.flab.ticketing.common.dto.CursorInfo
import com.flab.ticketing.performance.dto.PerformanceSearchConditions
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldNotBe

class PerformanceRepositoryImplTest(
    private val performanceRepository: PerformanceRepository
) : RepositoryTest() {

    init {

        "performanceRepository 객체를 정상적으로 주입받을 수 있다." {
            performanceRepository shouldNotBe null
        }

        "Performance List를 조회할 수 있다." {
            repeat(5) {
                performanceTestDataGenerator.saveTestPerformanceData()
            }

            val actual = performanceRepository.search(PerformanceSearchConditions(), CursorInfo())

            actual.filterNotNull().map { it.uid } shouldContainExactly listOf(
                "performance005",
                "performance004",
                "performance003",
                "performance002",
                "performance001"
            )
        }
    }

}