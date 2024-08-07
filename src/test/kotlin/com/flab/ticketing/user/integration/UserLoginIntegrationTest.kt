package com.flab.ticketing.user.integration

import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.user.dto.UserLoginDto
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.UserRepository
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import java.util.*
import java.util.regex.Pattern

class UserLoginIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var userRepository: UserRepository

    init {

        given("회원 가입이 완료된 사용자가") {
            val email = "email@email.com"
            val userPW = "abc1234!"
            userRepository.save(createUser(email, userPW))

            `when`("알맞은 Email과 Password를 입력하여 로그인을 시도할 시") {
                val uri = "/api/user/login"

                val dto = objectMapper.writeValueAsString(UserLoginDto(email, userPW))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("200 Success와 함께 JWT Token을 반환한다.") {
                    val actualJwt = mvcResult.response.getHeaderValue(HttpHeaders.AUTHORIZATION) as String

                    mvcResult.response.status shouldBeExactly 200
                    isJwt(actualJwt) shouldBe true
                }

            }

        }

        given("로그인 API가 POST로 설정되어 있을 때") {
            val email = "email@email.com"
            val userPW = "abc1234!"

            `when`("로그인 HTTP 메서드를 POST가 아닌 다른 메서드를 호출해 로그인을 시도한다면") {
                val uri = "/api/user/login"

                val dto = objectMapper.writeValueAsString(UserLoginDto(email, userPW))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()



                then("400 상태 코드와 적절한 오류를 리턴한다.") {
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    mvcResult.response.status shouldBe HttpStatus.BAD_REQUEST.value()
                    responseBody.code shouldBeEqual CommonErrorInfos.INVALID_METHOD.code
                    responseBody.message shouldBeEqual CommonErrorInfos.INVALID_METHOD.message
                }
            }
        }

    }

    private fun createUser(
        email: String,
        password: String,
        nickname: String = "Notused",
        uid: String = "NotUsed"
    ): User {
        return User(uid, email, passwordEncoder.encode(password), nickname)
    }

    private fun isJwt(token: String): Boolean {
        val parts = token.split(".")
        if (parts.size != 3) {
            return false
        }

        val base64UrlPattern = "^[A-Za-z0-9_-]+$"
        val regex = Pattern.compile(base64UrlPattern)

        return parts.all { regex.matcher(it).matches() && isBase64UrlDecodable(it) }

    }

    private fun isBase64UrlDecodable(str: String): Boolean {
        return try {
            Base64.getUrlDecoder().decode(str)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

}