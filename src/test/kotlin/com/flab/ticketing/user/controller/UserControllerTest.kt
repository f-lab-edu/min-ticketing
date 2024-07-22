package com.flab.ticketing.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.user.dto.UserEmailRegisterDto
import com.flab.ticketing.user.service.EmailService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.ints.shouldBeExactly
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
    private lateinit var emailService: EmailService


    init {
        given("이메일 인증 코드 전송 시도시") {
            val uri = "/api/user/new/email"

            `when`("이메일이 저장되어 있지 않다면") {
                val email = "noSaved@email.com"

                val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(email))
                every { emailService.sendEmail(email) } returns Unit

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(print())
                    .andReturn()


                `then`("200 코드를 반환하고 해당 이메일로 인증 코드를 전송한다.") {
                    mvcResult.response.status shouldBeExactly 200
                    verify { emailService.sendEmail(email) }
                }

            }

        }
    }
}