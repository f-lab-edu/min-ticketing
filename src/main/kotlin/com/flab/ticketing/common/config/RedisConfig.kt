package com.flab.ticketing.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory


@Configuration
class RedisConfig(
    @Value("\${spring.data.redis.host}") val host: String,
    @Value("\${spring.data.redis.port}") val port: Int
) {

    @Bean
    @Profile("prod")
    fun redisConnectionFactory(@Value("\${spring.data.redis.password}") password: String): RedisConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.hostName = host
        redisStandaloneConfiguration.port = port
        redisStandaloneConfiguration.setPassword(password)
        return LettuceConnectionFactory(redisStandaloneConfiguration)
    }

    @Bean
    @Profile("!prod")
    fun devRedisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory(host, port)
    }

}