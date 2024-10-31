package com.flab.ticketing.common.config

import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.util.DummySSLSocketFactory
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import redis.embedded.RedisServer
import java.security.Security

@TestConfiguration
class IntegrationTestConfig {

    @Bean
    fun redisServer(@Value("\${spring.data.redis.port}") port: Int): RedisServer {
        return RedisServer(port)
    }


    @Bean
    fun greenMail(
        @Value("\${spring.mail.port}") port: Int,
    ): GreenMail {
        Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory::class.qualifiedName)

        val smtpServer = ServerSetup(port, null, ServerSetup.PROTOCOL_SMTPS)
        val imapServer = ServerSetup(3026, null, ServerSetup.PROTOCOL_IMAP)

        val greenMail = GreenMail(arrayOf(smtpServer, imapServer))
            .withConfiguration(
                GreenMailConfiguration
                    .aConfig()
                    .withUser("foo@localhost", "foo", "foo-pwd")
            )
        return greenMail
    }
}