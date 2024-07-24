package com.flab.ticketing.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.user.dto.UserEmailRegisterDto
import com.flab.ticketing.user.dto.UserEmailVerificationDto
import com.flab.ticketing.user.exception.DuplicatedEmailException
import com.flab.ticketing.user.exception.InvalidEmailCodeException
import com.flab.ticketing.user.exception.NotFoundEmailCodeException
import com.flab.ticketing.user.exception.UserExceptionMessages
import com.flab.ticketing.user.service.UserService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import io.mockk.every
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.*

@WebMvcTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

                `then`("200 코드를 반환하고 해당 이메일로 인증 코드를 전송한다.") {
                    mvcResult.response.status shouldBeExactly 200
                    verify { userService.sendEmailVerifyCode(email) }
                }

            }

            `when`("이미 회원가입 되어있는 이메일이라면") {
                val email = "saved@email.com"
                val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(email))

                every { userService.sendEmailVerifyCode(email) } throws DuplicatedEmailException()

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
                    responseBody.message shouldBeEqual UserExceptionMessages.DUPLICATED_EMAIL.message
                }
            }

            `when`("이메일 형식이 올바르지 않다면") {
                val email = "s213"
                val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(email))

                every { userService.sendEmailVerifyCode(email) } throws DuplicatedEmailException()

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

                    mvcResult.response.status shouldBeExactly 400
                    responseBody.message shouldBeEqual UserExceptionMessages.EMAIL_EXPRESSION_INVALID.message

                }

            }

        }

        given("이메일 인증 코드 검증 요청 시") {
            val uri = "/api/user/new/email/verify"

            `when`("1시간 전에 인증 코드를 보냈던 이메일이라면") {
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

                every { userService.verifyEmailCode(email, code) } throws NotFoundEmailCodeException()

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


                    mvcResult.response.status shouldBeExactly 400
                    responseBody.message shouldBeEqual UserExceptionMessages.EMAIL_VERIFYCODE_NOT_FOUND.message
                }
            }

            `when`("사용자의 요청과 저장된 코드 정보가 다르다면") {
                val email = "noSaved@email.com"
                val code = "123ABC"
                val dto = objectMapper.writeValueAsString(UserEmailVerificationDto(email, code))

                every { userService.verifyEmailCode(email, code) } throws InvalidEmailCodeException()

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
                    responseBody.message shouldBeEqual UserExceptionMessages.EMAIL_VERIFYCODE_INVALID.message
                }
            }

            `when`("이메일 형식이 올바르지 않다면"){
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

                then("400 Bad Request를 반환한다."){
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    mvcResult.response.status shouldBeExactly 400
                    responseBody.message shouldBeEqual UserExceptionMessages.EMAIL_EXPRESSION_INVALID.message
                }
            }

        }

    }
}