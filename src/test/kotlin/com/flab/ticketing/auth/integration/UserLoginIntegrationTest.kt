package com.flab.ticketing.auth.integration

import com.flab.ticketing.auth.dto.UserInfoResponse
import com.flab.ticketing.auth.dto.request.UserLoginRequest
import com.flab.ticketing.auth.dto.request.UserPasswordUpdateRequest
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.testutils.IntegrationTest
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
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    init {

        given("유저 정보가 DB에 저장되어 있을 때 - 정상 처리") {
            val email = "email@email.com"
            val userPW = "abc1234!"
            userPersistenceUtils.saveNewUser(
                email = email,
                rawPassword = userPW
            )

            `when`("알맞은 Email과 Password를 입력하여 로그인을 시도할 시") {
                val uri = "/api/user/login"

                val dto = objectMapper.writeValueAsString(UserLoginRequest(email, userPW))

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

                val dto = objectMapper.writeValueAsString(UserLoginRequest(email, userPW))

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

            userPersistenceUtils.saveNewUser(
                email = email,
                rawPassword = userPW
            )

            `when`("비밀번호를 잘못 입력한 경우") {
                val uri = "/api/user/login"
                val invalidPW = "abcd1234!"
                val dto = objectMapper.writeValueAsString(UserLoginRequest(email, invalidPW))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()
                then("401 상태 코드와 적절한 메시지를 출력한다.") {
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, AuthErrorInfos.LOGIN_FAILED)
                }
            }
        }

        given("유저 정보가 DB에 저장되어 있지 않을 때 - 이메일 조회 불가") {


            `when`("비밀번호를 잘못 입력한 경우") {
                val uri = "/api/user/login"
                val email = "invalid@email.com"
                val userPW = "invalid1234!"

                val dto = objectMapper.writeValueAsString(UserLoginRequest(email, userPW))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()
                then("401 상태 코드와 적절한 메시지를 출력한다.") {
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, AuthErrorInfos.LOGIN_FAILED)
                }
            }
        }

        given("AccessToken을 가지고 있는 사용자가") {
            val email = "email@email.com"
            val userPW = "abc1234!"

            val (_, givenToken) = userPersistenceUtils.saveUserAndCreateJwt(email = email, rawPassword = userPW)

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
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, AuthErrorInfos.AUTH_INFO_INVALID)
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
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, AuthErrorInfos.AUTH_INFO_INVALID)
                }
            }
        }

        given("올바른 AccessToken을 가진 사용자가 - 인증타입 Bearer 미사용") {
            val email = "email@email.com"
            val userPW = "abc1234!"

            val (_, givenToken) = userPersistenceUtils.saveUserAndCreateJwt(email = email, rawPassword = userPW)

            `when`("인증 권한이 필요한 API 접근 시") {
                val uri = "/api/health-check"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Basic $givenToken")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("401 오류와 적절한 메시지를 출력한다.") {
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, AuthErrorInfos.AUTH_INFO_INVALID)

                }
            }
        }

        given("만료된 AccessToken을 가진 사용자가") {
            val email = "email@email.com"
            val userPW = "abc1234!"

            val (_, givenToken) = userPersistenceUtils.saveUserAndCreateJwt(
                email = email,
                rawPassword = userPW,
                createDate = Date(0)
            )

            `when`("인증이 필요한 API 접근 시") {
                val uri = "/api/health-check"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $givenToken")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("401 상태 코드와 적절한 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.UNAUTHORIZED, AuthErrorInfos.AUTH_INFO_EXPIRED)
                }
            }
        }

        given("회원 정보가 저장되어 있고, 해당 회원 정보로 인증된 사용자가") {
            val email = "email@email.com"
            val userPW = "abc1234!"

            val (_, givenToken) = userPersistenceUtils.saveUserAndCreateJwt(email = email, rawPassword = userPW)

            `when`("올바른 현재 비밀번호, 새 비밀번호, 새 비밀번호 확인을 입력해 비밀번호 업데이트를 시도할 시") {
                val uri = "/api/user/password"

                val newUserPW = "abcd1234!"

                val dto = objectMapper.writeValueAsString(UserPasswordUpdateRequest(userPW, newUserPW, newUserPW))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.patch(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $givenToken")
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("200 상태코드와 사용자의 비밀번호를 업데이트 한다.") {
                    mvcResult.response.status shouldBe HttpStatus.OK.value()

                    val actual = userRepository.findByEmail(email)!!.password
                    passwordEncoder.matches(newUserPW, actual) shouldBe true

                }
            }
        }

        given("회원 정보가 저장되어 있고, 해당 회원 정보로 인증된 사용자가 - 현재 비밀번호 오류") {
            val email = "email@email.com"
            val userPW = "abc1234!"

            val (_, givenToken) = userPersistenceUtils.saveUserAndCreateJwt(email = email, rawPassword = userPW)

            `when`("잘못된 현재 비밀번호, 올바른 새 비밀번호, 새 비밀번호 확인을 입력해 비밀번호 업데이트를 시도할 시") {
                val uri = "/api/user/password"

                val invalidCurrentPW = "abcde1234!"
                val newUserPW = "abcd1234!"

                val dto =
                    objectMapper.writeValueAsString(UserPasswordUpdateRequest(invalidCurrentPW, newUserPW, newUserPW))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.patch(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $givenToken")
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 상태 코드와 적절한 오류 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, AuthErrorInfos.PASSWORD_INVALID)

                }

            }
        }

        given("회원 정보가 저장되어 있고, 해당 회원 정보로 인증된 사용자가 - 새로운 비밀번호 형식 조건 불만족") {
            val email = "email@email.com"
            val userPW = "abc1234!"

            val (_, givenToken) = userPersistenceUtils.saveUserAndCreateJwt(email = email, rawPassword = userPW)

            `when`("영문, 숫자, 특수문자를 포함한 8글자 조건을 만족하지 못한 새 비밀번호로 변경을 시도할 시") {
                val uri = "/api/user/password"
                val newUserPW = "invalid"
                val dto = objectMapper.writeValueAsString(UserPasswordUpdateRequest(userPW, newUserPW, newUserPW))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.patch(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $givenToken")
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 상태코드와 적절한 오류 메시지를 반환한다.") {
                    checkError(
                        mvcResult,
                        HttpStatus.BAD_REQUEST,
                        CommonErrorInfos.INVALID_FIELD.code,
                        "newPassword,newPasswordConfirm" + CommonErrorInfos.INVALID_FIELD.message
                    )

                }
            }

        }

        given("회원 정보가 저장되어 있고, 해당 회원 정보로 인증된 사용자가 - 새로운 비밀번호와 새로운 비밀번호 불일치") {
            val email = "email@email.com"
            val userPW = "abc1234!"

            val (_, givenToken) = userPersistenceUtils.saveUserAndCreateJwt(email = email, rawPassword = userPW)

            `when`("서로 다른 newPassword와 newPasswordConfirm 입력시") {
                val uri = "/api/user/password"

                val newUserPW = "abcd1234!"
                val newUserPWConfirm = "abcde1234!"
                val dto =
                    objectMapper.writeValueAsString(UserPasswordUpdateRequest(userPW, newUserPW, newUserPWConfirm))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.patch(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $givenToken")
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("400 오류와 적절한 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, AuthErrorInfos.PASSWORD_CONFIRM_NOT_EQUALS)
                }
            }
        }

        given("회원가입과 로그인이 완료된 사용자가") {
            val userEmail = "email@email.com"
            val userPW = "abc1234!"
            val userUid = "userUid"
            val userNickname = "nickname"

            val (_, givenToken) = userPersistenceUtils.saveUserAndCreateJwt(
                uid = userUid,
                email = userEmail,
                rawPassword = userPW,
                nickname = userNickname
            )

            `when`("AccessToken을 사용하여 자신의 정보를 요청할 시") {
                val uri = "/api/user/info"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $givenToken")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("사용자의 정보를 반환한다.") {
                    mvcResult.response.status shouldBeExactly 200
                    val (id, nickname, email) = objectMapper.readValue(
                        mvcResult.response.contentAsString,
                        UserInfoResponse::class.java
                    )

                    id shouldBeEqual userUid
                    nickname shouldBeEqual userNickname
                    email shouldBeEqual userEmail
                }
            }
        }

        afterEach {
            userPersistenceUtils.clearContext()
        }
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