package com.flab.ticketing.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringTestExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import redis.embedded.RedisServer


@SpringBootTest
@Import(EmbeddedRedisServerConfig::class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class BehaviorIntegrationTest : BehaviorSpec() {
    override fun extensions(): List<Extension> = listOf(SpringTestExtension(SpringTestLifecycleMode.Root))

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper


    val greenMail = GreenMail(ServerSetupTest.SMTP_IMAP)
        .withConfiguration(
            GreenMailConfiguration
                .aConfig()
                .withUser("foo@localhost", "foo", "foo-pwd")

        )

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        greenMail.start()
    }

    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        greenMail.stop()
    }

}


@TestConfiguration
internal class EmbeddedRedisServerConfig(
    @Value("\${spring.data.redis.port}") val port: Int
) {

    private val redisServer = RedisServer(port)

    @PostConstruct
    fun runRedisServer() {
        redisServer.start()
    }

    @PreDestroy
    fun stopRedisServer() {
        redisServer.stop()
    }

}