package com.flab.ticketing.batch.migration

import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import com.flab.ticketing.performance.repository.PerformanceSearchRepository
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.JpaPagingItemReader
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager


@Configuration
class PerformanceMigrationJobConfig(
    private val jobRepository: JobRepository,
    private val emf: EntityManagerFactory,
    @Value("\${spring.batch.performance-migration.chunk-size}") private val chunkSize: Int
) {

    @Bean
    fun performanceMigrationJob(
        @Qualifier("performanceMigrationStep") readPerformanceStep: Step
    ): Job {
        return JobBuilder("performanceMigrationJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(readPerformanceStep)
            .build()

    }

    @Bean("performanceMigrationStep")
    fun performanceMigrationStep(
        trasactionManager: PlatformTransactionManager,
        @Qualifier("performanceItemReader") itemReader: ItemReader<Performance>,
        @Qualifier("performanceConvertProcessor") itemProcessor: ItemProcessor<Performance, PerformanceSearchSchema>,
        @Qualifier("performanceItemWriter") itemWriter: ItemWriter<PerformanceSearchSchema>
    ): Step {

        return StepBuilder("performanceMigrationStep", jobRepository)
            .chunk<Performance, PerformanceSearchSchema>(chunkSize, trasactionManager)
            .reader(itemReader)
            .processor(itemProcessor)
            .writer(itemWriter)
            .build()
    }

    @Bean("performanceItemReader")
    fun performanceItemReader(): JpaPagingItemReader<Performance> {
        return JpaPagingItemReaderBuilder<Performance>()
            .name("performanceItemReader")
            .queryString("select p From Performance p join fetch p.performanceDateTime order by p.id asc")
            .entityManagerFactory(emf)
            .pageSize(chunkSize)
            .build()
    }

    @Bean("performanceConvertProcessor")
    fun performanceConvertItemProcessor(): PerformanceConvertItemProcessor {
        return PerformanceConvertItemProcessor()
    }

    @Bean("performanceItemWriter")
    fun performanceItemWriter(
        performanceSearchRepository: PerformanceSearchRepository
    ): ItemWriter<PerformanceSearchSchema> {
        return PerformanceItemWriter(performanceSearchRepository)
    }


}