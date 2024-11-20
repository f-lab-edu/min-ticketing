package com.flab.ticketing.common.config

import org.apache.http.ssl.SSLContextBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.springframework.data.elasticsearch.support.HttpHeaders
import java.security.cert.X509Certificate


@Configuration
@EnableElasticsearchRepositories
class ElasticSearchConfig {


    //    @Configuration
//    @Profile("dev")
    internal class LocalElasticSearchConfig(
        @Value("\${spring.data.elasticsearch.url}") private val elasticHost: String
    ) : ElasticsearchConfiguration() {

        // 로컬 개발 용 ElasticSearch 설정으로, HTTPS를 Disabled 하였습니다.
        override fun clientConfiguration(): ClientConfiguration {
            return ClientConfiguration.builder()
                .connectedTo(elasticHost)
                .build()
        }

    }

    @Configuration
//    @Profile("prod")
    internal class ProductionElasticSearchConfig(
        @Value("\${spring.data.elasticsearch.url}") private val elasticHost: String,
        @Value("\${spring.data.elasticsearch.api-key}") private val elasticApiKey: String
    ) : ElasticsearchConfiguration() {

        override fun clientConfiguration(): ClientConfiguration {
            val httpHeaders = HttpHeaders()
            httpHeaders.add("Authorization", "ApiKey $elasticApiKey")
            // SSL Context 설정
            val sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null) { _: Array<X509Certificate>, _: String -> true }
                .build()

            return ClientConfiguration.builder()
                .connectedTo(elasticHost)
                .usingSsl(sslContext)
                .withDefaultHeaders(httpHeaders)
                .build()
        }

    }
}