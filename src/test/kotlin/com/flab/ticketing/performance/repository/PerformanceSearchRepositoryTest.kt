package com.flab.ticketing.performance.repository

import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.testcontainers.elasticsearch.ElasticsearchContainer


@SpringBootTest(classes = [ElasticSearchConfiguration::class, PerformanceSearchRepository::class])
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class PerformanceSearchRepositoryTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)


    @Autowired
    private lateinit var performanceSearchRepository: PerformanceSearchRepository


    init {
        "공연 정보를 조건 없이 조회할 수 있다." {
            // given
            val performanceDocuments =
                PerformanceTestDataGenerator.createPerformanceGroupbyRegion(performanceCount = 5)
                    .map { PerformanceSearchSchema.of(it) }

            performanceSearchRepository.saveAll(performanceDocuments)

            //when
            val foundDocuments = performanceSearchRepository.findAll()

            //then
            foundDocuments shouldContainAll performanceDocuments
        }
    }

    override suspend fun beforeEach(testCase: TestCase) {
        super.beforeEach(testCase)
        withContext(Dispatchers.IO) {
            performanceSearchRepository.deleteAll()
        }
    }

}


@TestConfiguration
@EnableElasticsearchRepositories
internal class ElasticSearchConfiguration : ElasticsearchConfiguration() {

    @Bean(initMethod = "init", destroyMethod = "preDestroy")
    fun elasticsearchTestContainer() = ElasticSearchTestContainerWrapper()

    override fun clientConfiguration(): ClientConfiguration {
        val esTestContainer = elasticsearchTestContainer()

        return ClientConfiguration.builder()
            .connectedTo(esTestContainer.getHttpHostAddress())
            .build()
    }
}

internal class ElasticSearchTestContainerWrapper {

    private val elasticsearchContainer = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.13.4")

    fun init() {
        elasticsearchContainer
            .apply {
                withEnv("xpack.security.enabled", "false")
                withEnv("discovery.type", "single-node")
                start()
            }
    }

    fun preDestroy() {
        elasticsearchContainer.stop()
    }


    fun getHttpHostAddress(): String = elasticsearchContainer.httpHostAddress
}