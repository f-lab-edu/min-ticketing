package com.flab.ticketing.performance.repository

import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
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


/**
 * CI/CD 환경에서는 Docker를 활용한 TestContainer 사용이 불가능 하기 때문에 로컬에서만 테스트하도록 설정하였습니다.
 * */
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

        "DB에 Performance List가 limit 이상의 갯수를 저장하고 있다면, 올바르게 limit개 만큼의 데이터를 갖고 올 수 있다." {

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 10,
                numShowtimes = 1,
                seatPerPlace = 1
            ).map { PerformanceSearchSchema.of(it) }


            performanceSearchRepository.saveAll(performances)

            val limit = 5
            val actual = performanceSearchRepository.search(PerformanceSearchConditions(), null, limit)

            actual.second.size shouldBe limit
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