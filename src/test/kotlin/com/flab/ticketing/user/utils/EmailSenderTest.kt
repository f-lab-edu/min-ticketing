package com.flab.ticketing.user.utils

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

class EmailSenderTest : BehaviorSpec(){

    val javaEmailSender : JavaMailSender = mockk()

    val emailSender : EmailSender = EmailSender(
        senderEmail = "noreply@minticketing.com",
        mailSender = javaEmailSender
    )

    init {
        given("이메일 전송 시 "){

            `when`("알맞은 이메일과 내용을 입력하면"){
                val email = "email@email.com"
                val subject = "제목"
                val content = "내용"

                every { javaEmailSender.send(ofType<SimpleMailMessage>()) } returns Unit
                emailSender.sendEmail(email, subject, content)


                `then`("이메일을 전송할 수 있다."){
                    verify { javaEmailSender.send(ofType<SimpleMailMessage>()) }
                }
            }
        }
    }

}
