package com.flab.ticketing.user.utils

import com.flab.ticketing.common.UnitTest
import io.kotest.matchers.shouldBe
import org.springframework.security.core.GrantedAuthority
import java.util.*
import java.util.regex.Pattern

class JwtTokenProviderTest : UnitTest() {
    private val jwtTokenProvider = JwtTokenProvider(SECRET_KEY, 1000000L)

    companion object {
        private const val SECRET_KEY =
            "SECRETKEYSECRETKEYSECRETKEYSECRETKEYSECRETKEYSECRETKEYSECRETKEYSECRETKEYSECRETKEY"
    }

    init {
        "JWT 토큰을 생성할 수 있다." {
            val email = "email@email.com"
            val authorities = mutableListOf<GrantedAuthority>()


            val jwt = jwtTokenProvider.sign(email, authorities)


            isJwt(jwt) shouldBe true

        }
    }


    private fun isJwt(token: Any?): Boolean {
        if (token == null || token !is String) {
            return false
        }
        val parts = token.split(".")
        if (parts.size != 3) {
            return false
        }

        val base64UrlPattern = "^[A-Za-z0-9_-]+$"
        val regex = Pattern.compile(base64UrlPattern)

        return parts.all { regex.matcher(it).matches() && isBase64UrlDecodable(it) }

    }

    private fun isBase64UrlDecodable(str: String): Boolean {
        return try {
            Base64.getUrlDecoder().decode(str)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
