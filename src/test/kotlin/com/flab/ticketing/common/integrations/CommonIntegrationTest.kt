package com.flab.ticketing.common.integrations

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.testutils.IntegrationTest
import com.flab.ticketing.testutils.generator.UserTestDataGenerator
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.user.repository.UserRepository
import io.kotest.core.test.TestCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import java.util.*

class CommonIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

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
            val user = UserTestDataGenerator.createUser()

            userRepository.save(user)

            val token = jwtTokenProvider.sign(
                AuthenticatedUserDto(user.uid, user.email, user.nickname),
                mutableListOf(),
                Date()
            )
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
        userRepository.deleteAll()
    }
}