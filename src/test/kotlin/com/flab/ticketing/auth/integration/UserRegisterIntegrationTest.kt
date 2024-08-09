package com.flab.ticketing.auth.integration

import com.flab.ticketing.auth.dto.UserEmailRegisterDto
import com.flab.ticketing.auth.dto.UserEmailVerificationDto
import com.flab.ticketing.auth.dto.UserRegisterDto
import com.flab.ticketing.auth.entity.EmailVerifyInfo
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.auth.repository.EmailVerifyInfoRepository
import com.flab.ticketing.auth.repository.UserRepository
import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.user.entity.User
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import jakarta.mail.Folder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers

class UserRegisterIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var emailRepository: EmailVerifyInfoRepository

    init {
        given("올바른 형식의 이메일이 주어졌을 때") {

            val email = "foo@localhost"

            `when`("이메일 인증 코드를 요청 시") {
                val uri = "/api/user/new/email"

                val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(email))


                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("200 코드를 반환하고 해당 이메일로 인증 코드를 전송한다.") {
                    mvcResult.response.status shouldBeExactly 200

                    greenMail.waitForIncomingEmail(1)
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

            `when`("이메일 인증 코드를 요청 시") {
                val uri = "/api/user/new/email"
                val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(from))


                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 Bad Request와 알맞은 메시지를 출력한다.") {
                    checkError(
                        mvcResult,
                        HttpStatus.BAD_REQUEST,
                        CommonErrorInfos.INVALID_FIELD.code,
                        "email" + CommonErrorInfos.INVALID_FIELD.message
                    )
                }
            }

        }
        given("이미 가입된 이메일이 주어졌을 때") {
            val savedEmail = "test@email.com"

            userRepository.save(createUser(savedEmail))

            `when`("이메일 인증 코드를 요청 시") {
                val uri = "/api/user/new/email"
                val dto = objectMapper.writeValueAsString(UserEmailRegisterDto(savedEmail))


                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("409 상태코드와 관련한 메시지를 리턴한다.") {
                    checkError(mvcResult, HttpStatus.CONFLICT, AuthErrorInfos.DUPLICATED_EMAIL)
                }
            }

        }

        given("올바른 형식의 이메일이고, 인증코드가 유효할 때 - 정상 인증") {
            val email = "good@email.com"
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


                then("검증을 완료상태로 변경하고 200 코드를 반환한다.") {
                    val actual = emailRepository.findById(email).get()

                    mvcResult.response.status shouldBeExactly 200
                    actual.isVerified shouldBeEqual true

                }
            }

        }
        given("올바른 형식의 이메일이고, 인증코드가 유효할 때 - 인증 코드 불일치") {
            val email = "good@email.com"
            val code = "1234AB"

            emailRepository.save(EmailVerifyInfo(email, code))

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
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, AuthErrorInfos.EMAIL_VERIFYCODE_INVALID)
                }
            }
        }

        given("인증 코드를 보낸적 없는 이메일이거나 만료된 인증 코드일 때") {
            val email = "noSaved@email.com"
            val code = "123ABC"

            `when`("인증 코드를 검증 시도하면") {
                val uri = "/api/user/new/email/verify"
                val dto = objectMapper.writeValueAsString(UserEmailVerificationDto(email, code))

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(dto)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("404 상태코드와 관련 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.NOT_FOUND, AuthErrorInfos.EMAIL_VERIFY_INFO_NOT_FOUND)
                }
            }
        }

        given("이메일 인증 코드 인증이 완료된 사용자의 경우 - 정상 회원가입") {
            val email = "email@email.com"
            emailRepository.save(EmailVerifyInfo(email, "noUsed", true))

            `when`("영문, 숫자, 특수문자를 1글자씩 포함한 8자 이상의 비밀번호와, 동일한 비밀번호 확인, 닉네임을 입력해 회원가입을 완료할 시") {
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

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("정상 처리되어 200 OK 상태 코드를 반환한다.") {
                    mvcResult.response.status shouldBeExactly 200
                    userRepository.findByEmail(email) shouldNotBe null
                }
            }
        }
        given("이메일 인증 코드 인증이 완료된 사용자의 경우 - 비밀번호 불일치") {
            val email = "email@email.com"
            emailRepository.save(EmailVerifyInfo(email, "noUsed", true))

            `when`("다른 비밀번호화 비밀번호 확인을 입력하여 회원가입을 완료할 시") {
                val uri = "/api/user/new/info"

                val userPW = "abc1234!"
                val userPWConfirm = "1234abc!"
                val nickname = "minturtle"
                val dto = UserRegisterDto(
                    email,
                    userPW,
                    userPWConfirm,
                    nickname
                )

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 상태코드와 알맞은 메시지를 출력한다.") {
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, AuthErrorInfos.PASSWORD_CONFIRM_NOT_EQUALS)
                    userRepository.findByEmail(email) shouldBe null
                }
            }

        }
        given("이메일 인증 코드 인증이 완료된 사용자의 경우 - 비밀번호 조건 불만족") {
            val email = "email@email.com"
            emailRepository.save(EmailVerifyInfo(email, "noUsed", true))

            forAll(
                row("Short1!", "Short1!"),
                row("OnlyEnglish", "OnlyEnglish"),
                row("12345678", "12345678"),
                row("!@#$%^&*", "!@#$%^&*"),
                row("NoSpecial123", "NoSpecial123"),
                row("NoNumber!", "NoNumber!"),
                row("1234!@#$", "1234!@#$")
            ) { userPW, userPWConfirm ->
                `when`("Password가 조건을 만족하지 못하는 경우") {
                    val uri = "/api/user/new/info"

                    val nickname = "minturtle"
                    val dto = UserRegisterDto(
                        email,
                        userPW,
                        userPWConfirm,
                        nickname
                    )

                    val mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.post(uri)
                            .content(objectMapper.writeValueAsString(dto))
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                        .andDo(MockMvcResultHandlers.print())
                        .andReturn()

                    then("400 상태 코드와 적절한 오류 정보를 반환한다.") {
                        checkError(
                            mvcResult,
                            HttpStatus.BAD_REQUEST,
                            CommonErrorInfos.INVALID_FIELD.code,
                            "password" + CommonErrorInfos.INVALID_FIELD.message
                        )

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

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 상태코드와 알맞은 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, AuthErrorInfos.EMAIL_VERIFY_INFO_NOT_FOUND)
                    userRepository.findByEmail(email) shouldBe null
                }
            }
        }
        given("이메일 인증 메일은 전송하였으나, 메일 인증을 완료하지 않은 사용자의 경우") {
            val email = "notVerified@email.com"

            emailRepository.save(EmailVerifyInfo(email, "noUsed"))

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

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 상태코드와 적절한 상태 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, AuthErrorInfos.EMAIL_NOT_VERIFIED)
                    userRepository.findByEmail(email) shouldBe null
                }
            }
        }

        beforeSpec {
            greenMail.start()
        }

        afterContainer {
            greenMail.reset()
            userRepository.deleteAll()
        }

        afterSpec {
            greenMail.stop()
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