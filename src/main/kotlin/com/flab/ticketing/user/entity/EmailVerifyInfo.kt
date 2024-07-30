package com.flab.ticketing.user.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash


@RedisHash(value = "emailVerifyInfo", timeToLive = 3600)
class EmailVerifyInfo(
    @Id
    val email: String,
    val code: String,
    val isVerified: Boolean = false
)