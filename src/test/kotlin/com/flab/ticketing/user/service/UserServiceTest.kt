package com.flab.ticketing.user.service

import com.flab.ticketing.user.repository.EmailRepository
import com.flab.ticketing.user.repository.UserRepository
import com.flab.ticketing.user.utils.EmailCodeGenerator
import com.flab.ticketing.user.utils.EmailSender
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



    }

}