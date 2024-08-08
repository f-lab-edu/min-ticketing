package com.flab.ticketing.auth.integration

import com.flab.ticketing.auth.dto.UserLoginDto
import com.flab.ticketing.auth.entity.User
import com.flab.ticketing.auth.exception.UserErrorInfos
import com.flab.ticketing.auth.repository.UserRepository
import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.exception.CommonErrorInfos
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

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    init {

        given("유저 정보가 DB에 저장되어 있을 때 - 정상 처리") {
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
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, CommonErrorInfos.INVALID_METHOD)
                }
            }
        }

        given("유저 정보가 DB에 저장되어 있을 때 - 비밀번호 입력 오류") {
            val email = "email@email.com"
            val userPW = "abc1234!"
            userRepository.save(createUser(email, userPW))

            `when`("비밀번호를 잘못 입력한 경우") {
                val uri = "/api/user/login"
                val invalidPW = "abcd1234!"
                val dto = objectMapper.writeValueAsString(UserLoginDto(email, invalidPW))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()
                then("401 상태 코드와 적절한 메시지를 출력한다.") {
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, UserErrorInfos.LOGIN_FAILED)
                }
            }
        }

        given("유저 정보가 DB에 저장되어 있지 않을 때 - 이메일 조회 불가") {


            `when`("비밀번호를 잘못 입력한 경우") {
                val uri = "/api/user/login"
                val email = "invalid@email.com"
                val password = "invalid1234!"

                val dto = objectMapper.writeValueAsString(UserLoginDto(email, password))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()
                then("401 상태 코드와 적절한 메시지를 출력한다.") {
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, UserErrorInfos.LOGIN_FAILED)
                }
            }
        }

        given("AccessToken을 가지고 있는 사용자가") {
            val email = "email@email.com"
            val password = "abc1234!"

            userRepository.save(createUser(email, password))

            val givenToken = jwtTokenProvider.sign(email, mutableListOf())

            `when`("인증 권한이 필요한 API 접근 시") {
                val uri = "/api/health-check"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $givenToken")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("인증되어 해당 API에 접근할 수 있다.") {
                    mvcResult.response.status shouldBe 200
                }
            }

        }

        given("AccessToken을 가지지 않은 사용자가") {

            `when`("인증이 필요한 API 접근시") {
                val uri = "/api/health-check"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("401 상태 코드와 적절한 오류 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, UserErrorInfos.AUTH_INFO_INVALID)
                }
            }
        }

        given("잘못된 AccessToken을 가진 사용자가") {
            val invalidToken =
                "eyJhbGciOiJIUz42AVJ9.eyJzdWIiOiJlbASSSFGASFSA5jb20iLCJhdXRoIjoiIiwiZXhwIjoxNzIzMTAwNjAwfQ.uaSN-VfV4A7ASDASDA23qKA4vB_i8f0Q2HFEmjzvmCCntpYFiCs71ioqJVlJ4ioTdJY"

            `when`("인증이 필요한 API 접근시") {
                val uri = "/api/health-check"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $invalidToken")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("401 오류와 적절한 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, UserErrorInfos.AUTH_INFO_INVALID)
                }
            }
        }

        given("올바른 AccessToken을 가진 사용자가 - 인증타입 Bearer 미사용") {
            val email = "email@email.com"
            val password = "abc1234!"

            userRepository.save(createUser(email, password))

            val givenToken = jwtTokenProvider.sign(email, mutableListOf())

            `when`("인증 권한이 필요한 API 접근 시") {
                val uri = "/api/health-check"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Basic $givenToken")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("401 오류와 적절한 메시지를 출력한다.") {
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, UserErrorInfos.AUTH_INFO_INVALID)

                }
            }
        }

        given("만료된 AccessToken을 가진 사용자가") {
            val email = "email@email.com"
            val password = "abc1234!"

            userRepository.save(createUser(email, password))

            val givenToken = jwtTokenProvider.sign(email, mutableListOf(), Date(0))
            `when`("인증이 필요한 API 접근 시") {
                val uri = "/api/health-check"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $givenToken")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("401 상태 코드와 적절한 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, UserErrorInfos.AUTH_INFO_EXPIRED)
                }
            }
        }

        afterEach {
            userRepository.deleteAll()
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