package com.flab.ticketing.common

import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import com.flab.ticketing.performance.repository.dsl.CustomPerformanceRepositoryImpl
import com.linecorp.kotlinjdsl.support.spring.data.jpa.autoconfigure.KotlinJdslAutoConfiguration
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(
    KotlinJdslAutoConfiguration::class,
    CustomPerformanceRepositoryImpl::class,
    RepositoryTest.RepositoryTestConfig::class
)
abstract class RepositoryTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var performanceTestDataGenerator: PerformanceTestDataGenerator

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)
        performanceTestDataGenerator.reset()
    }

    @TestConfiguration
    internal class RepositoryTestConfig {

        @Bean
        fun testDataGenerator(
            regionRepository: RegionRepository,
            placeRepository: PerformancePlaceRepository,
            performanceRepository: PerformanceRepository
        ): PerformanceTestDataGenerator {

            return PerformanceTestDataGenerator(
                regionRepository,
                placeRepository,
                performanceRepository
            )
        }

    }
}

