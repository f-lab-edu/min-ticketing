package com.flab.ticketing.common.integrations

import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.testutils.IntegrationTest
import io.kotest.core.test.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print

class CommonIntegrationTest : IntegrationTest() {

    init {

        given("특정 URL이 존재하지 않을 때") {
            `when`("사용자가 해당 URL에 접근하고자 하면") {
                val uri = "/api/notused"

                val mvcResult = mockMvc.perform(get(uri))
                    .andDo(print())
                    .andReturn()

                then("404 오류를 throw한다.") {
                    checkError(mvcResult, HttpStatus.NOT_FOUND, CommonErrorInfos.NOT_FOUND)

                }
            }
        }


        given("특정 URL이 존재하지 않을 때 - 인증된 사용자") {
            val (_, token) = userTestUtils.saveUserAndCreateJwt()

            `when`("사용자가 해당 URL에 접근하고자 하면") {
                val uri = "/api/notused"

                val mvcResult = mockMvc.perform(
                    get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${token}")
                )
                    .andDo(print())
                    .andReturn()

                then("404 오류를 throw한다.") {
                    checkError(mvcResult, HttpStatus.NOT_FOUND, CommonErrorInfos.NOT_FOUND)

                }
            }
        }
    }


    override suspend fun beforeEach(testCase: TestCase) {
        withContext(Dispatchers.IO) {
            userTestUtils.clearContext()
        }
    }
}