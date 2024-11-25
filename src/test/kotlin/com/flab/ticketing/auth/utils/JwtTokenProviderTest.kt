package com.flab.ticketing.auth.utils

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.testutils.UnitTest
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
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
            // given
            val uid = "uid"
            val email = "email@email.com"
            val nickname = "nickname"

            val authenticatedUserDto = AuthenticatedUserDto(uid, email, nickname)
            val authorities = mutableListOf<GrantedAuthority>()

            // when
            val jwt = jwtTokenProvider.sign(authenticatedUserDto, authorities)

            // then
            isJwt(jwt) shouldBe true

        }

        "생성한 JWT 토큰을 resolve해 Authentication 객체로 생성할 수 있다." {
            // given
            val uid = "uid"
            val email = "email@email.com"
            val nickname = "nickname"
            val authenticatedUserDto = AuthenticatedUserDto(uid, email, nickname)

            val role = SimpleGrantedAuthority("ROLE_USER")
            val authorities = mutableListOf<GrantedAuthority>(role)

            // when
            val jwt = jwtTokenProvider.sign(authenticatedUserDto, authorities)

            // then
            val authentication = jwtTokenProvider.getAuthentication(jwt)

            val principal = authentication.principal

            principal.shouldBeInstanceOf<AuthenticatedUserDto>()

            principal.uid shouldBeEqual uid
            principal.email shouldBeEqual email
            principal.nickname shouldBeEqual nickname

            authentication.authorities shouldContainExactly listOf(role)
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