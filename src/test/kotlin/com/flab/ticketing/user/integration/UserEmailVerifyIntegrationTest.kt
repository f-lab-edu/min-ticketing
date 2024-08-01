package com.flab.ticketing.user.integration

import com.flab.ticketing.common.BehaviorIntegrationTest
import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.user.dto.UserEmailRegisterDto
import com.flab.ticketing.user.dto.UserEmailVerificationDto
import com.flab.ticketing.user.entity.EmailVerifyInfo
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.exception.UserErrorInfos
import com.flab.ticketing.user.repository.EmailVerifyInfoRepository
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

    @Autowired
    private lateinit var emailRepository: EmailVerifyInfoRepository

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

        given("올바른 형식의 이메일이고, 인증코드가 유효할 때") {
            val email = "foo@localhost"
            val code = "1234AB"

            emailRepository.save(EmailVerifyInfo(email, code))

            `when`("올바른 인증 코드를 가지고 검증 시도할 시") {
                val uri = "/api/user/new/email/verify"
                val dto = objectMapper.writeValueAsString(UserEmailVerificationDto(email, code))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                `then`("검증이 완료되어 200 코드를 반환한다.") {
                    val actual = emailRepository.findById(email).get()

                    mvcResult.response.status shouldBeExactly 200
                    actual.isVerified shouldBeEqual true

                }
            }
            `when`("잘못된 인증 코드를 가지고 검증 시도할 시") {
                val invalidCode = "AB1234"
                val dto = objectMapper.writeValueAsString(UserEmailVerificationDto(email, invalidCode))
                val uri = "/api/user/new/email/verify"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 상태코드와 알맞은 메시지를 반환한다.") {
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    mvcResult.response.status shouldBeExactly 400
                    responseBody.message shouldBeEqual UserErrorInfos.EMAIL_VERIFYCODE_INVALID.message
                    responseBody.code shouldBeEqual UserErrorInfos.EMAIL_VERIFYCODE_INVALID.code
                }
            }
        }
        given("인증 코드를 보낸적 없는 이메일이거나 만료된 인증 코드일 때") {
            val email = "noSaved@email.com"
            val code = "123ABC"
            val dto = objectMapper.writeValueAsString(UserEmailVerificationDto(email, code))


            `when`("인증 코드를 검증 시도하면") {
                val uri = "/api/user/new/email/verify"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("404 상태코드와 관련 메시지를 반환한다.") {
                    val responseBody =
                        objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)

                    mvcResult.response.status shouldBeExactly 404
                    responseBody.message shouldBeEqual UserErrorInfos.EMAIL_VERIFY_INFO_NOT_FOUND.message
                    responseBody.code shouldBeEqual UserErrorInfos.EMAIL_VERIFY_INFO_NOT_FOUND.code
                }
            }
        }


        afterSpec {
            greenMail.reset()
            emailRepository.deleteAll()
        }
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