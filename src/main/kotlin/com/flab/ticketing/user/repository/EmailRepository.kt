package com.flab.ticketing.user.repository

import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration
import java.time.temporal.ChronoUnit

class EmailRepository(
    private val redisTemplate: RedisTemplate<String, String>
) {


    fun saveCode(email: String, code: String) {
        redisTemplate.opsForValue().set(email, code, Duration.of(1, ChronoUnit.HOURS))
    }

    fun getCode(email: String): String? {
        return redisTemplate.opsForValue().get(email)
    }
}