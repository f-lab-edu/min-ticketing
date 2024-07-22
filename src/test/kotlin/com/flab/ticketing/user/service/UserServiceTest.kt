package com.flab.ticketing.user.service

import com.flab.ticketing.user.exception.NotFoundEmailCodeException
import com.flab.ticketing.user.repository.EmailRepository
import com.flab.ticketing.user.repository.UserRepository
import com.flab.ticketing.user.utils.EmailCodeGenerator
import com.flab.ticketing.user.utils.EmailSender
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UserServiceTest : BehaviorSpec(){


    private val emailCodeGenerator: EmailCodeGenerator = mockk()

    private val emailSender: EmailSender = mockk()

    private val emailRepository : EmailRepository = mockk()

    private val userRepository : UserRepository = mockk()
    private val userService: UserService = UserService(emailCodeGenerator, emailSender, emailRepository, userRepository)

    init {

        given("이메일 인증 코드 요청시"){

            `when`("알맞은 이메일 양식이 오면"){
                val email = "email@email.com"
                val code = "123abc"

                every {emailCodeGenerator.createEmailCode() } returns code
                every {emailSender.sendEmail(any(), any(), any())} returns Unit
                every { emailRepository.saveCode(any(), any()) } returns Unit
                userService.sendEmailVerifyCode(email)

                `then`("이메일을 전송할 수 있다."){
                    verify { emailSender.sendEmail(email, "Min-Ticketing 인증 이메일", "MinTicketing 이메일 인증 코드는 $code 입니다.") }
                    verify { emailRepository.saveCode(email, code) }
                }
            }

        }

        given("이메일 인증 코드 검증 요청 시"){

            `when`("이메일 Repository에 이메일 코드가 존재하고, 사용자의 요청과 일치한다면"){
                val email = "email@email.com"
                val code = "123abc"

                every { emailRepository.getCode(email) } returns code

                then("오류를 반환하지 않는다."){
                    shouldNotThrow<Exception> {
                        userService.verifyEmailCode(email, code)
                    }
                }
            }

            `when`("이메일 Repository에 이메일 코드가 존재하지 않는다면"){
                val email = "email@email.com"
                val code = "123abc"

                every { emailRepository.getCode(email) } returns null

                then("InvalidEmailCodeException을 반환한다."){
                    shouldThrow<NotFoundEmailCodeException> {
                        userService.verifyEmailCode(email, code)
                    }
                }
            }
        }


    }

}