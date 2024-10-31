package com.flab.ticketing.common.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.testcontainers.elasticsearch.ElasticsearchContainer


@TestConfiguration
@EnableElasticsearchRepositories
class ElasticSearchConfiguration : ElasticsearchConfiguration() {

    @Bean(initMethod = "init", destroyMethod = "preDestroy")
    fun elasticsearchTestContainer() = ElasticSearchTestContainerWrapper()

    override fun clientConfiguration(): ClientConfiguration {
        val esTestContainer = elasticsearchTestContainer()

        return ClientConfiguration.builder()
            .connectedTo(esTestContainer.getHttpHostAddress())
            .build()
    }
}

class ElasticSearchTestContainerWrapper {

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