package com.flab.ticketing.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.springframework.data.elasticsearch.support.HttpHeaders


@Configuration
@EnableElasticsearchRepositories
class ElasticSearchConfig {


    @Configuration
    @Profile("!prod")
    internal class LocalElasticSearchConfig(
        @Value("\${spring.data.elasticsearch.url}") private val elasticHost: String,
        @Value("\${spring.data.elasticsearch.api-key}") private val elasticApiKey: String
    ) : ElasticsearchConfiguration() {

        // 로컬 개발 용 ElasticSearch 설정으로, HTTPS를 Disabled 하였습니다.
        override fun clientConfiguration(): ClientConfiguration {
            val httpHeaders = HttpHeaders()
            httpHeaders.add("Authorization", "ApiKey $elasticApiKey")
            return ClientConfiguration.builder()
                .connectedTo(elasticHost)
                .withDefaultHeaders(httpHeaders)
                .build()
        }

    }

    @Configuration
    @Profile("prod")
    internal class ProductionElasticSearchConfig(
        @Value("\${spring.data.elasticsearch.url}") private val elasticHost: String,
        @Value("\${spring.data.elasticsearch.api-key}") private val elasticApiKey: String
    ) : ElasticsearchConfiguration() {

        override fun clientConfiguration(): ClientConfiguration {
            val httpHeaders = HttpHeaders()
            httpHeaders.add("Authorization", "ApiKey $elasticApiKey")
            return ClientConfiguration.builder()
                .connectedTo(elasticHost)
                .usingSsl()
                .withDefaultHeaders(httpHeaders)
                .build()
        }

    }
}