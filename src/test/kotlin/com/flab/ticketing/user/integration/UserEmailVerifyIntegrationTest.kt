package com.flab.ticketing.user.integration

import com.flab.ticketing.common.BehaviorIntegrationTest
import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.user.dto.UserEmailRegisterDto
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.exception.UserErrorInfos
import com.flab.ticketing.user.repository.UserRepository
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.string.shouldMatch
import jakarta.mail.Folder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers

class UserEmailVerifyIntegrationTest : BehaviorIntegrationTest() {

    @Autowired
    private lateinit var userRepository: UserRepository

    init {
        given("올바른 형식의 이메일이 주어졌을 때") {

            val from = "foo@localhost"
            val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(from))

            `when`("이메일 인증 코드를 요청 시") {
                val uri = "/api/user/new/email"

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
        given("잘못된 형식의 이메일이 주어졌을 때") {
            val from = "wrong"
            val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(from))
            `when`("이메일 인증 코드를 요청 시") {
                val uri = "/api/user/new/email"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 Bad Request와 알맞은 메시지를 출력한다.") {
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)
                    val expectedMessage = "email 필드의 값이 올바르지 않습니다."

                    mvcResult.response.status shouldBeExactly 400
                    responseBody.message shouldBeEqual expectedMessage
                    responseBody.code shouldBeEqual CommonErrorInfos.INVALID_FIELD.code

                }
            }

        }
        given("이미 가입된 이메일이 주어졌을 때") {
            val savedEmail = "test@email.com"
            userRepository.save(createUser(savedEmail))
            val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(savedEmail))

            `when`("이메일 인증 코드를 요청 시") {
                val uri = "/api/user/new/email"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("409 상태코드와 관련한 메시지를 리턴한다.") {
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    mvcResult.response.status shouldBeExactly 409
                    responseBody.message shouldBeEqual UserErrorInfos.DUPLICATED_EMAIL.message
                    responseBody.code shouldBeEqual UserErrorInfos.DUPLICATED_EMAIL.code
                }
            }

        }

        afterEach { greenMail.reset() }
    }


    fun createUser(
        email: String,
        password: String = "abc1234!",
        nickname: String = "tester",
        uid: String = "testUid"
    ): User {
        return User(uid, email, password, nickname)
    }
}