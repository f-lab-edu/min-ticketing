package com.flab.ticketing.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.dto.response.ErrorResponse
import com.flab.ticketing.common.exception.ErrorInfo
import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.util.DummySSLSocketFactory
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import redis.embedded.RedisServer
import java.security.Security


@SpringBootTest
@Import(IntegrationTestConfig::class, MockServerUtils::class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTest : BehaviorSpec() {
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


@TestConfiguration
internal class IntegrationTestConfig {

    @Bean
    fun redisServer(@Value("\${spring.data.redis.port}") port: Int): RedisServer {
        return RedisServer(port)
    }


    @Bean
    fun greenMail(
        @Value("\${spring.mail.port}") port: Int,
    ): GreenMail {
        Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory::class.qualifiedName)

        val smtpServer = ServerSetup(port, null, ServerSetup.PROTOCOL_SMTPS)
        val imapServer = ServerSetup(3026, null, ServerSetup.PROTOCOL_IMAP)

        val greenMail = GreenMail(arrayOf(smtpServer, imapServer))
            .withConfiguration(
                GreenMailConfiguration
                    .aConfig()
                    .withUser("foo@localhost", "foo", "foo-pwd")
            )
        return greenMail
    }
}