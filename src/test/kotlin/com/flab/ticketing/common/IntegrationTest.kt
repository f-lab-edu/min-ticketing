package com.flab.ticketing.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.dto.response.ErrorResponse
import com.flab.ticketing.common.exception.ErrorInfo
import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import redis.embedded.RedisServer


@SpringBootTest
@Import(EmbeddedRedisServerConfig::class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTest : BehaviorSpec() {
    override fun extensions(): List<Extension> = listOf(SpringExtension)

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


    protected fun checkError(mvcResult: MvcResult, expectedStatus: HttpStatus, expectedErrorInfo: ErrorInfo) {
        checkError(mvcResult, expectedStatus, expectedErrorInfo.code, expectedErrorInfo.message)
    }

    protected fun checkError(
        mvcResult: MvcResult,
        expectedStatus: HttpStatus,
        expectedCode: String,
        expectedMessage: String
    ) {
        val responseBody =
            objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

        mvcResult.response.status shouldBeExactly expectedStatus.value()
        responseBody.code shouldBeEqual expectedCode
        responseBody.message shouldBeEqual expectedMessage
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