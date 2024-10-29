package com.flab.ticketing.batch.migration

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager


@Configuration
class PerformanceMigrationJobConfig(
    @Autowired private val jobRepository: JobRepository
) {

    @Bean
    fun performanceMigrationJob(
        @Qualifier("readPerformanceStep") readPerformanceStep: Step
    ): Job {
        return JobBuilder("performanceMigrationJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(readPerformanceStep)
            .build()

    }

    @Bean
    fun readPerformanceStep(
        trasactionManager: PlatformTransactionManager
    ): Step {
        var i = 0
        return StepBuilder("readPerformanceStep", jobRepository)
            .chunk<Int, String>(200, trasactionManager)
            .reader {
                i++
                when (i < 500) {
                    true -> i
                    false -> null
                }
            }
            .writer { println(" >>> ${it}") }
            .build()
    }

}