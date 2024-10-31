package com.flab.ticketing.batch.migration

import com.flab.ticketing.common.BatchIntegrationTest
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.JpaPagingItemReader
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.JobRepositoryTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value


class PerformanceMigrationBatchTest(
    @Value("\${spring.batch.performance-migration.chunk-size}") private val chunkSize: Int
) : BatchIntegrationTest() {


    @Autowired
    private lateinit var jobRepositoryTestUtils: JobRepositoryTestUtils

    @Autowired
    private lateinit var jobLauncher: JobLauncher

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Qualifier("performanceMigrationJob")
    @Autowired
    private lateinit var job: Job

    @Autowired
    @Qualifier("performanceItemReader")
    private lateinit var itemReader: JpaPagingItemReader<Performance>

    @Autowired
    private lateinit var regionRepository: RegionRepository

    @Autowired
    private lateinit var placeRepository: PerformancePlaceRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    @Autowired
    private lateinit var jobLauncherTestUtils : JobLauncherTestUtils

    init {
        "Performance 데이터를 읽어서 ElasticSearch에 저장할 수 있다." {
            // given
            val performanceCount = chunkSize * 2
            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = performanceCount
            )

            savePerformance(performances)
            every { performanceSearchRepository.saveAll(any<List<PerformanceSearchSchema>>()) } returns listOf()
            // when
            val execution = jobLauncherTestUtils.launchStep("performanceMigrationStep")

            //then
            execution.exitStatus shouldBe ExitStatus.COMPLETED
            verify(exactly = 2) { performanceSearchRepository.saveAll(any<List<PerformanceSearchSchema>>()) }

        }

        "Performance 데이터를 읽을 수 있다." {
            // given
            val performanceCount = 10
            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = performanceCount
            )

            savePerformance(performances)
            itemReader.open(ExecutionContext())
            // when & then
            for (i in 0..<performanceCount) {

                val read = itemReader.read()
                read shouldNotBe null
            }
            itemReader.close()
        }
    }


    override suspend fun beforeEach(testCase: TestCase) {
        jobRepositoryTestUtils.removeJobExecutions()
        jobLauncherTestUtils = setUpPerformanceMigrationJobLaunchTestUtils()

    }


    private fun setUpPerformanceMigrationJobLaunchTestUtils(): JobLauncherTestUtils {
        val jobLauncherTestUtils = JobLauncherTestUtils()
        jobLauncherTestUtils.jobLauncher = jobLauncher
        jobLauncherTestUtils.job = job
        jobLauncherTestUtils.jobRepository = jobRepository

        return jobLauncherTestUtils
    }


    private fun savePerformance(performances: List<Performance>) {
        regionRepository.save(performances[0].performancePlace.region)
        placeRepository.save(performances[0].performancePlace)

        performances.forEach {
            performanceRepository.save(it)
        }
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)

        PerformanceTestDataGenerator.reset()
        withContext(Dispatchers.IO) {
            performanceRepository.deleteAll()
            placeRepository.deleteAll()
            regionRepository.deleteAll()
        }


    }

}