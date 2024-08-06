package com.flab.ticketing.user.utils

import com.flab.ticketing.common.UnitTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

class EmailSenderTest : UnitTest() {

    val javaEmailSender: JavaMailSender = mockk()
    val emailSender: EmailSender = EmailSender(
        senderEmail = "noreply@minticketing.com",
        mailSender = javaEmailSender
    )

    init {
        "수신자 이메일과 제목, 내용을 입력해 이메일을 전송할 수 있다." {
            val email = "email@email.com"
            val title = "제목"
            val content = "내용"

            every { javaEmailSender.send(ofType<SimpleMailMessage>()) } returns Unit
            emailSender.sendEmail(email, title, content)

            verify { javaEmailSender.send(ofType<SimpleMailMessage>()) }
        }

    }

}
