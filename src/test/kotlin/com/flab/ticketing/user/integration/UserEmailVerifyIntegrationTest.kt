package com.flab.ticketing.user.integration

import com.flab.ticketing.common.BehaviorIntegrationTest
import com.flab.ticketing.user.dto.UserEmailRegisterDto
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.string.shouldMatch
import jakarta.mail.Folder
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers

class UserEmailVerifyIntegrationTest : BehaviorIntegrationTest() {

    init {
        given("올바른 형식의 이메일이 주어졌을 때") {


            val uri = "/api/user/new/email"
            val from = "foo@localhost"
            val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(from))

            `when`("이메일 인증 코드를 요청 시") {

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("200 코드를 반환하고 해당 이메일로 인증 코드를 전송한다.") {
                    greenMail.waitForIncomingEmail(1)

                    mvcResult.response.status shouldBeExactly 200

                    greenMail.imap.createStore().use { store ->
                        run {
                            store.connect("foo", "foo-pwd")
                            val inbox = store.getFolder("INBOX")
                            inbox.open(Folder.READ_ONLY)

                            inbox.messageCount shouldBeExactly 1
                            inbox.messages[0].content.toString() shouldMatch """MinTicketing 이메일 인증 코드는 [A-Z0-9]{6} 입니다\."""
                        }

                    }
                }
            }
        }

        afterEach { greenMail.reset() }
    }
}