package com.flab.ticketing.auth.service

import com.flab.ticketing.auth.dto.UserRegisterDto
import com.flab.ticketing.auth.repository.UserRepository
import com.flab.ticketing.auth.utils.EmailCodeGenerator
import com.flab.ticketing.auth.utils.EmailSender
import com.flab.ticketing.auth.utils.EmailVerifier
import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.user.entity.User
import io.kotest.assertions.throwables.shouldNotThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest : UnitTest() {
    private val emailCodeGenerator: EmailCodeGenerator = mockk()
    private val emailSender: EmailSender = mockk()
    private val emailVerifier: EmailVerifier = mockk()
    private val userRepository: UserRepository = mockk()
    private val userPWEncoder: PasswordEncoder = mockk()
    private val nanoIdGenerator: NanoIdGenerator = mockk()
    private val authService: AuthService =
        AuthService(emailCodeGenerator, emailSender, emailVerifier, userRepository, userPWEncoder, nanoIdGenerator)

    init {
        "email 인증 정보가 저장되어 있지 않은 이메일에 인증 코드를 생성해 메일을 보낼 수 있다." {
            val email = "email@email.com"
            val code = "123abc"
            every { emailCodeGenerator.createEmailCode() } returns code
            every { emailSender.sendEmail(any(), any(), any()) } returns Unit
            every { emailVerifier.saveCode(any(), any()) } returns Unit
            every { userRepository.findByEmail(any()) } returns null

            authService.sendEmailVerifyCode(email)

            verify {
                emailSender.sendEmail(
                    email,
                    "Min-Ticketing 인증 이메일",
                    "MinTicketing 이메일 인증 코드는 $code 입니다."
                )
            }
            verify { emailVerifier.saveCode(email, code) }
        }

        "email 인증 정보가 저장된 유저가 인증 코드 검증 요청시 exception을 throw하지 않고 검증을 완료한다." {
            val email = "email@email.com"
            val code = "123abc"
            every { emailVerifier.getCode(email) } returns code
            every { emailVerifier.setVerifySuccess(email) } returns Unit

            shouldNotThrow<Exception> {
                authService.verifyEmailCode(email, code)
            }
            verify { emailVerifier.setVerifySuccess(email) }

        }

        "이메일 검증 정보가 존재할 때 해당 이메일로 추가 정보를 입력할 시 사용자 정보를 DB에 저장한다." {
            val email = "email@email.com"
            val userPW = "abc1234!"
            val userPWConfirm = "abc1234!"
            val nickname = "minturtle"
            val encryptedUserPW = "asldll321lslafas231412@3@!Ffa"
            val uid = "123asf"
            val expectedUser = User(uid, email, encryptedUserPW, nickname)

            every { emailVerifier.checkVerified(email) } returns Unit
            every { userPWEncoder.encode(userPW) } returns encryptedUserPW
            every { nanoIdGenerator.createNanoId() } returns uid
            every { userRepository.save(any()) } returns expectedUser

            authService.saveVerifiedUserInfo(
                UserRegisterDto(
                    email,
                    userPW,
                    userPWConfirm,
                    nickname
                )
            )

            verify { userRepository.save(expectedUser) }
        }


    }

}