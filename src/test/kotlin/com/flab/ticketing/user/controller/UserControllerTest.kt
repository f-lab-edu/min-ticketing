package com.flab.ticketing.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.config.SecurityConfig
import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.common.exception.*
import com.flab.ticketing.user.dto.UserEmailRegisterDto
import com.flab.ticketing.user.dto.UserEmailVerificationDto
import com.flab.ticketing.user.dto.UserRegisterDto
import com.flab.ticketing.user.exception.UserErrorInfos.*
import com.flab.ticketing.user.service.UserService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import io.mockk.every
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print

@WebMvcTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfig::class)
class UserControllerTest : BehaviorSpec() {
    override fun extensions(): List<Extension> = listOf(SpringExtension)

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var userService: UserService


    init {
        given("이메일 인증 코드 전송 시도시") {
            val uri = "/api/user/new/email"

            `when`("이메일이 저장되어 있지 않다면") {
                val email = "noSaved@email.com"
                val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(email))

                every { userService.sendEmailVerifyCode(email) } returns Unit

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("200 코드를 반환하고 해당 이메일로 인증 코드를 전송한다.") {
                    mvcResult.response.status shouldBeExactly 200
                    verify { userService.sendEmailVerifyCode(email) }
                }

            }

            `when`("이미 회원가입 되어있는 이메일이라면") {
                val email = "saved@email.com"
                val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(email))

                every { userService.sendEmailVerifyCode(email) } throws DuplicatedException(DUPLICATED_EMAIL)

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("409 오류를 리턴한다.") {
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    mvcResult.response.status shouldBeExactly 409
                    responseBody.message shouldBeEqual DUPLICATED_EMAIL.message
                    responseBody.code shouldBeEqual DUPLICATED_EMAIL.code
                }
            }

            `when`("이메일 형식이 올바르지 않다면") {
                val email = "s213"
                val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(email))

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("400 Bad Request를 반환한다.") {
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    val expectedMessage = "email 필드의 값이 올바르지 않습니다."

                    mvcResult.response.status shouldBeExactly 400
                    responseBody.message shouldBeEqual expectedMessage
                    responseBody.code shouldBeEqual CommonErrorInfos.INVALID_FIELD.code
                }

            }

        }

        given("이메일 인증 코드 검증 요청 시") {
            val uri = "/api/user/new/email/verify"

            `when`("유효기간 이내에 인증코드를 보냈던 이메일이라면") {
                val email = "noSaved@email.com"
                val code = "123ABC"

                val dto = objectMapper.writeValueAsString(UserEmailVerificationDto(email, code))

                every { userService.verifyEmailCode(email, code) } returns Unit

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("200 OK를 반환한다.") {
                    mvcResult.response.status shouldBeExactly 200
                    verify { userService.verifyEmailCode(email, code) }
                }
            }

            `when`("1시간이 지났거나 잘못된 코드인 경우") {
                val email = "noSaved@email.com"
                val code = "123ABC"
                val dto = objectMapper.writeValueAsString(UserEmailVerificationDto(email, code))

                every {
                    userService.verifyEmailCode(
                        email,
                        code
                    )
                } throws NotFoundException(EMAIL_VERIFY_INFO_NOT_FOUND)

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("404 Not Found를 반환한다.") {
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)


                    mvcResult.response.status shouldBeExactly 404
                    responseBody.message shouldBeEqual EMAIL_VERIFY_INFO_NOT_FOUND.message
                    responseBody.code shouldBeEqual EMAIL_VERIFY_INFO_NOT_FOUND.code
                }
            }

            `when`("사용자의 요청과 저장된 코드 정보가 다르다면") {
                val email = "noSaved@email.com"
                val code = "123ABC"
                val dto = objectMapper.writeValueAsString(UserEmailVerificationDto(email, code))

                every {
                    userService.verifyEmailCode(
                        email,
                        code
                    )
                } throws InvalidValueException(EMAIL_VERIFYCODE_INVALID)

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("400 BadRequest를 반환한다.") {
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    mvcResult.response.status shouldBeExactly 400
                    responseBody.message shouldBeEqual EMAIL_VERIFYCODE_INVALID.message
                    responseBody.code shouldBeEqual EMAIL_VERIFYCODE_INVALID.code
                }
            }

            `when`("이메일 형식이 올바르지 않다면") {
                val email = "asdasd"
                val code = "123ABC"
                val dto = objectMapper.writeValueAsString(UserEmailVerificationDto(email, code))

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("400 Bad Request를 반환한다.") {
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    val expectedMessage = "email 필드의 값이 올바르지 않습니다."

                    mvcResult.response.status shouldBeExactly 400
                    responseBody.message shouldBeEqual expectedMessage
                    responseBody.code shouldBeEqual CommonErrorInfos.INVALID_FIELD.code
                }
            }

        }

        given("이메일 인증 코드 인증이 완료된 사용자의 경우") {
            val uri = "/api/user/new/info"
            val email = "email@email.com"


            `when`("추가 개인 정보를 입력하여 회원가입을 완료할 시") {

                val userPW = "abc1234!"
                val userPWConfirm = "abc1234!"
                val nickname = "minturtle"
                val dto = UserRegisterDto(
                    email,
                    userPW,
                    userPWConfirm,
                    nickname
                )

                every { userService.saveVerifiedUserInfo(dto) } returns Unit

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("정상 처리되어 200 OK 상태 코드를 반환한다.") {
                    mvcResult.response.status shouldBeExactly 200
                    verify { userService.saveVerifiedUserInfo(dto) }
                }
            }

            `when`("다른 userPW와 userPWConfirm을 입력하여 회원가입을 완료할 시") {
                val userPW = "abc1234!"
                val userPWConfirm = "1234abc!"
                val nickname = "minturtle"
                val dto = UserRegisterDto(
                    email,
                    userPW,
                    userPWConfirm,
                    nickname
                )

                every { userService.saveVerifiedUserInfo(dto) } throws InvalidValueException(PASSWORD_CONFIRM_NOT_EQUALS)

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("400 상태코드와 알맞은 메시지를 출력한다.") {
                    mvcResult.response.status shouldBeExactly 400

                    val responseBody = objectMapper.readValue(
                        mvcResult.response.contentAsString,
                        ErrorResponse::class.java
                    )


                    responseBody.message shouldBeEqual PASSWORD_CONFIRM_NOT_EQUALS.message
                    responseBody.code shouldBeEqual PASSWORD_CONFIRM_NOT_EQUALS.code
                }
            }

            forAll(
                row("Short1!", "Short1!"),
                row("NoNumbers!", "NoNumbers!"),
                row("12345678", "12345678"),
                row("!@#$%^&*", "!@#$%^&*"),
                row("NoSpecial123", "NoSpecial123"),
                row("NoNumber!", "NoNumber!"),
                row("1234!@#$", "1234!@#$")
            ) { userPW, userPWConfirm ->
                `when`("Password가 조건을 만족하지 못하는 경우") {
                    val nickname = "minturtle"
                    val dto = UserRegisterDto(
                        email,
                        userPW,
                        userPWConfirm,
                        nickname
                    )

                    val mvcResult = mockMvc.perform(
                        post(uri)
                            .content(objectMapper.writeValueAsString(dto))
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                        .andDo(print())
                        .andReturn()

                    then("400 상태 코드와 적절한 오류 정보를 반환한다.") {
                        mvcResult.response.status shouldBeExactly 400

                        val responseBody = objectMapper.readValue(
                            mvcResult.response.contentAsString,
                            ErrorResponse::class.java
                        )

                        responseBody.code shouldBeEqual CommonErrorInfos.INVALID_FIELD.code
                        responseBody.message shouldBeEqual "password" + CommonErrorInfos.INVALID_FIELD.message
                    }
                }
            }

        }

        given("이메일 인증 시도를 하지 않았거나 가입 유효시간이 지난 사용자의 경우") {
            val email = "noSaved@email.com"


            `when`("추가 개인 정보를 입력하여 회원가입을 시도할 시") {
                val uri = "/api/user/new/info"

                val userPW = "abc1234!"
                val userPWConfirm = "abc1234!"
                val nickname = "minturtle"
                val dto = UserRegisterDto(
                    email,
                    userPW,
                    userPWConfirm,
                    nickname
                )

                every { userService.saveVerifiedUserInfo(dto) } throws ForbiddenException(EMAIL_VERIFY_INFO_NOT_FOUND)

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("403 상태코드와 알맞은 메시지를 반환한다.") {
                    mvcResult.response.status shouldBeExactly 403

                    val responseBody = objectMapper.readValue(
                        mvcResult.response.contentAsString,
                        ErrorResponse::class.java
                    )

                    responseBody.message shouldBeEqual EMAIL_VERIFY_INFO_NOT_FOUND.message
                    responseBody.code shouldBeEqual EMAIL_VERIFY_INFO_NOT_FOUND.code
                }
            }
        }

        given("이메일 인증 메일은 전송하였으나, 메일 인증을 완료하지 않은 사용자의 경우") {
            val email = "notVerified@email.com"

            `when`("추가 개인 정보를 입력하여 회원가입을 시도할 시") {
                val uri = "/api/user/new/info"

                val userPW = "abc1234!"
                val userPWConfirm = "abc1234!"
                val nickname = "minturtle"
                val dto = UserRegisterDto(
                    email,
                    userPW,
                    userPWConfirm,
                    nickname
                )

                every { userService.saveVerifiedUserInfo(dto) } throws ForbiddenException(EMAIL_VERIFY_NOT_COMPLETED)

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()

                then("403 상태코드와 적절한 상태 메시지를 반환한다.") {
                    mvcResult.response.status shouldBeExactly 403

                    val responseBody = objectMapper.readValue(
                        mvcResult.response.contentAsString,
                        ErrorResponse::class.java
                    )

                    responseBody.code shouldBeEqual EMAIL_VERIFY_NOT_COMPLETED.code
                    responseBody.message shouldBeEqual EMAIL_VERIFY_NOT_COMPLETED.message
                }
            }
        }
    }
}