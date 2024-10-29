package com.flab.ticketing.common

import com.flab.ticketing.performance.repository.PerformanceSearchRepository
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.StringSpec
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource


@SpringBatchTest
@SpringBootTest
@Import(BatchTestConfig::class)
@ActiveProfiles("test")
abstract class BatchIntegrationTest : StringSpec() {

    @MockkBean
    private lateinit var performanceSearchRepository: PerformanceSearchRepository

}

@TestConfiguration
internal class BatchTestConfig {

    @Bean
    fun dataSource(): DataSource {
        return EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("/org/springframework/batch/core/schema-h2.sql")
            .build()
    }
}