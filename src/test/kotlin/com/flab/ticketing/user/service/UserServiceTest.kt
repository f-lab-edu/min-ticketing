package com.flab.ticketing.user.service

import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.user.dto.UserRegisterDto
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.exception.UserErrorInfos
import com.flab.ticketing.user.repository.EmailVerifier
import com.flab.ticketing.user.repository.UserRepository
import com.flab.ticketing.user.utils.EmailCodeGenerator
import com.flab.ticketing.user.utils.EmailSender
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest : BehaviorSpec() {
    private val emailCodeGenerator: EmailCodeGenerator = mockk()
    private val emailSender: EmailSender = mockk()
    private val emailVerifier: EmailVerifier = mockk()
    private val userRepository: UserRepository = mockk()
    private val userPWEncoder: PasswordEncoder = mockk()
    private val nanoIdGenerator: NanoIdGenerator = mockk()
    private val userService: UserService =
        UserService(emailCodeGenerator, emailSender, emailVerifier, userRepository, userPWEncoder, nanoIdGenerator)

    init {

        given("이메일 인증 코드 요청시") {
            `when`("알맞은 이메일 양식이 오면") {
                val email = "email@email.com"
                val code = "123abc"

                every { emailCodeGenerator.createEmailCode() } returns code
                every { emailSender.sendEmail(any(), any(), any()) } returns Unit
                every { emailVerifier.saveCode(any(), any()) } returns Unit
                every { userRepository.findByEmail(any()) } returns null
                userService.sendEmailVerifyCode(email)

                `then`("이메일을 전송할 수 있다.") {
                    verify {
                        emailSender.sendEmail(
                            email,
                            "Min-Ticketing 인증 이메일",
                            "MinTicketing 이메일 인증 코드는 $code 입니다."
                        )
                    }
                    verify { emailVerifier.saveCode(email, code) }
                }
            }

        }

        given("이메일 인증 코드 검증 요청 시") {
            `when`("이메일 Repository에 이메일 코드가 존재하고, 사용자의 요청과 일치한다면") {
                val email = "email@email.com"
                val code = "123abc"

                every { emailVerifier.getCode(email) } returns code
                every { emailVerifier.setVerifySuccess(email) } returns Unit
                then("오류를 반환하지 않는다.") {
                    shouldNotThrow<Exception> {
                        userService.verifyEmailCode(email, code)
                    }
                    verify { emailVerifier.setVerifySuccess(email) }
                }
            }

            `when`("이메일 Repository에 이메일 코드가 존재하지 않는다면") {
                val email = "email@email.com"
                val code = "123abc"

                every { emailVerifier.getCode(email) } returns null

                then("NotFoundException과 알맞은 메시지를 반환한다.") {
                    val exception = shouldThrow<NotFoundException> {
                        userService.verifyEmailCode(email, code)
                    }

                    exception.info.message shouldBeEqual UserErrorInfos.EMAIL_VERIFY_INFO_NOT_FOUND.message
                }
            }

            `when`("파라미터의 code와 조회된 코드가 다르다면") {
                val email = "email@email.com"
                val code = "123abc"
                val savedCode = "abc123"

                every { emailVerifier.getCode(email) } returns savedCode

                then("InvalidValueException을 반환한다.") {
                    val exception = shouldThrow<InvalidValueException> {
                        userService.verifyEmailCode(email, code)
                    }

                    exception.info.message shouldBeEqual UserErrorInfos.EMAIL_VERIFYCODE_INVALID.message
                }
            }

        }

        given("이메일 인증 코드 검증이 완료된 사용자의 경우") {
            val email = "email@email.com"

            every { emailVerifier.checkVerified(email) } returns Unit

            `when`("정상적인 추가 개인 정보를 입력하여 회원가입을 완료할 시") {
                val userPW = "abc1234!"
                val userPWConfirm = "abc1234!"
                val nickname = "minturtle"

                val encrypteduserPW = "asldll321lslafas231412@3@!Ffa"
                val uid = "123asf"

                val dto = UserRegisterDto(
                    email,
                    userPW,
                    userPWConfirm,
                    nickname
                )

                val expectedUser = User(uid, email, encrypteduserPW, nickname)

                every { userPWEncoder.encode(userPW) } returns encrypteduserPW
                every { nanoIdGenerator.createNanoId() } returns uid
                every { userRepository.save(any()) } returns expectedUser



                userService.saveVerifiedUserInfo(dto)

                then("DB에 회원 정보를 추가해 저장할 수 있다.") {
                    verify { userRepository.save(expectedUser) }
                }
            }
            `when`("userPW와 userPW Confirm을 잘못입력하여 회원가입을 완료할 시") {
                val userPW = "abc1234!"
                val userPWConfirm = "123123abc!"
                val nickname = "minturtle"
                val dto = UserRegisterDto(
                    email,
                    userPW,
                    userPWConfirm,
                    nickname
                )

                then("InvalidValueException과 그에 맞는 오류 정보를 담아 throw한다.") {
                    val e = shouldThrow<InvalidValueException> { userService.saveVerifiedUserInfo(dto) }
                    e.info shouldBeEqual UserErrorInfos.PASSWORD_CONFIRM_NOT_EQUALS

                }
            }

        }

    }

}