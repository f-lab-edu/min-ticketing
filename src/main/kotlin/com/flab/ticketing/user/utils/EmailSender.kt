package com.flab.ticketing.user.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class EmailSender(
    @Value("\${spring.mail.sender}") private val senderEmail: String,
    private val mailSender: JavaMailSender
) {

    fun sendEmail(email: String, subject: String, content: String) {
        val message = SimpleMailMessage()
        message.from = senderEmail
        message.setTo(email)
        message.text = content

        mailSender.send(message)
    }
}