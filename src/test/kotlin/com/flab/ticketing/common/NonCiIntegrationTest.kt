package com.flab.ticketing.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.conditions.NonCiEnvironment
import com.flab.ticketing.common.config.ElasticSearchConfiguration
import com.flab.ticketing.common.config.IntegrationTestConfig
import com.icegreen.greenmail.util.GreenMail
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import redis.embedded.RedisServer


@SpringBootTest
@Import(IntegrationTestConfig::class, MockServerUtils::class, ElasticSearchConfiguration::class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIf(NonCiEnvironment::class)
abstract class NonCiIntegrationTest : BehaviorSpec() {

    override fun extensions(): List<Extension> = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var mockServerUtils: MockServerUtils

    @Autowired
    lateinit var greenMail: GreenMail

    @Autowired
    private lateinit var redisServer: RedisServer

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        redisServer.start()
        greenMail.start()
        mockServerUtils.runServer()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        redisServer.stop()
        greenMail.stop()
        mockServerUtils.shutDown()
    }


}