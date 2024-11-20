package com.flab.ticketing.testutils

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.dto.response.ErrorResponse
import com.flab.ticketing.common.exception.ErrorInfo
import com.flab.ticketing.performance.repository.PerformanceSearchRepository
import com.flab.ticketing.testutils.config.IntegrationTestConfig
import com.flab.ticketing.testutils.config.TestUtilConfig
import com.flab.ticketing.testutils.persistence.PerformanceTestUtils
import com.flab.ticketing.testutils.persistence.UserTestUtils
import com.icegreen.greenmail.util.GreenMail
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import redis.embedded.RedisServer


@SpringBootTest
@Import(IntegrationTestConfig::class, MockServerUtils::class, TestUtilConfig::class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTest : BehaviorSpec() {
    override fun extensions(): List<Extension> = listOf(SpringExtension)

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var mockServerUtils: MockServerUtils

    @Autowired
    protected lateinit var greenMail: GreenMail

    @Autowired
    protected lateinit var userTestUtils: UserTestUtils

    @Autowired
    protected lateinit var performanceTestUtils: PerformanceTestUtils

    @Autowired
    private lateinit var redisServer: RedisServer

    @MockkBean
    private lateinit var performanceSearchRepository: PerformanceSearchRepository

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