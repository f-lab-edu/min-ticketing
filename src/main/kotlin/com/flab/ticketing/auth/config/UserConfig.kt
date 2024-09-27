package com.flab.ticketing.auth.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*


@Configuration
class UserConfig {

    @Bean
    fun javaMailSender(
        @Value("\${spring.mail.host}") host: String?,
        @Value("\${spring.mail.port}") port: Int,
        @Value("\${spring.mail.username}") username: String?,
        @Value("\${spring.mail.password}") password: String?
    ): JavaMailSender {

        val javaMailSender = JavaMailSenderImpl()
        javaMailSender.host = host
        javaMailSender.port = port
        javaMailSender.username = username
        javaMailSender.password = password
        javaMailSender.setJavaMailProperties(getMailProperties())

        return javaMailSender
    }

    private fun getMailProperties(): Properties {
        val properties = Properties()
        properties.setProperty("mail.transport.protocol", "smtp")
        properties.setProperty("mail.debug", "false")
        properties.setProperty("mail.smtp.auth", "true")
        properties.setProperty("mail.smtp.starttls.enable", "true")
        properties.setProperty("mail.smtp.connectiontimeout", "5000")
        properties.setProperty("mail.smtp.timeout", "3000")
        properties.setProperty("mail.smtp.writetimeout", "5000")
        return properties
    }

}