package com.flab.ticketing.batch.migration

import com.flab.ticketing.common.BatchIntegrationTest
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.JobRepositoryTestUtils
import org.springframework.beans.factory.annotation.Qualifier


class PerformanceMigrationBatchTest(
    private val jobRepositoryTestUtils: JobRepositoryTestUtils,
    private val jobLauncher: JobLauncher,
    private val jobRepository: JobRepository,
    @Qualifier("performanceMigrationJob") private val job: Job
) : BatchIntegrationTest() {

    private val jobLauncherTestUtils = setUpPerformanceMigrationJobLaunchTestUtils()


    init {
        "정상적으로 PerformanceMigrationBatchTest을 실행하고 종료할 수 있다." {
            // given
            // when
            val execution = jobLauncherTestUtils.launchJob()

            // then
            execution.exitStatus shouldBe ExitStatus.COMPLETED


        }

    }


    override suspend fun beforeEach(testCase: TestCase) {
        jobRepositoryTestUtils.removeJobExecutions()
    }


    private fun setUpPerformanceMigrationJobLaunchTestUtils(): JobLauncherTestUtils {
        val jobLauncherTestUtils = JobLauncherTestUtils()
        jobLauncherTestUtils.jobLauncher = jobLauncher
        jobLauncherTestUtils.job = job
        jobLauncherTestUtils.jobRepository = jobRepository

        return jobLauncherTestUtils
    }
}