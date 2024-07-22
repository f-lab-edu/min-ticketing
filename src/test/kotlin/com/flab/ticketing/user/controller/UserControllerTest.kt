package com.flab.ticketing.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.user.dto.UserEmailRegisterDto
import com.flab.ticketing.user.exception.DuplicatedEmailException
import com.flab.ticketing.user.exception.UserExceptionMessages
import com.flab.ticketing.user.service.UserService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.*

@SpringBootTest
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

            `when`("이미 회원가입 되어있는 이메일이라면"){
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

                `then`("409 오류를 리턴한다."){
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    mvcResult.response.status shouldBeExactly 409
                    responseBody.message shouldBeEqual UserExceptionMessages.DUPLICATED_EMAIL.message
                }
            }

            `when`("이메일 형식이 올바르지 않다면"){
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

                `then`("400 Bad Request를 반환한다."){
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    mvcResult.response.status shouldBeExactly 400
                    responseBody.message shouldBeEqual UserExceptionMessages.EMAIL_EXPRESSION_INVALID.message

                }

            }

        }
    }
}