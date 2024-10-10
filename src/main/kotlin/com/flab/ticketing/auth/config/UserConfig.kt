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
        javaMailSender.javaMailProperties = getMailProperties(host)

        return javaMailSender
    }

    private fun getMailProperties(host: String?): Properties {
        val props = Properties()
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.ssl.enable"] = "true"
        props["mail.smtp.ssl.trust"] = host
        return props
    }

}