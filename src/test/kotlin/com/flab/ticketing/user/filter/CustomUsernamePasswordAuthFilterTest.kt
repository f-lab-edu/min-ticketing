package com.flab.ticketing.user.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.user.dto.UserLoginDto
import com.flab.ticketing.user.utils.JwtTokenProvider
import com.flab.ticketing.user.utils.UserLoginInfoConverter
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.web.HttpRequestMethodNotSupportedException
import java.nio.charset.StandardCharsets


class CustomUsernamePasswordAuthFilterTest : UnitTest() {
    private val authManager: AuthenticationManager = mockk()
    private val userLoginInfoConverter: UserLoginInfoConverter = mockk()
    private val jwtTokenProvider: JwtTokenProvider = mockk()
    private val objectMapper: ObjectMapper = mockk()
    private val usernamePasswordAuthFilter =
        CustomUsernamePasswordAuthFilter(authManager, userLoginInfoConverter, jwtTokenProvider, objectMapper)

    init {
        "Request Body로 부터 사용자의 id와 password를 파싱하고 Authentication 토큰을 넘겨줄 수 있다." {
            val email = "email@email.com"
            val password = "abc1234!@"

            val request: HttpServletRequest = mockk()
            val response: HttpServletResponse = mockk()

            every { request.method } returns "POST"
            every { request.inputStream } returns mockk()

            every { request.characterEncoding } returns StandardCharsets.UTF_8.name()
            every { userLoginInfoConverter.convert(any(), StandardCharsets.UTF_8) } returns UserLoginDto(
                email,
                password
            )
            every { authManager.authenticate(any()) } returns mockk()

            usernamePasswordAuthFilter.attemptAuthentication(request, response)

            verify {
                authManager.authenticate(match {
                    it.principal.equals(email) && it.credentials.equals(password)
                })
            }

        }

        "사용자가 POST가 아닌 method를 입력할 시 HttpRequestMethodNotSupportedException를 throw 한다." {
            val request: HttpServletRequest = mockk()
            val response: HttpServletResponse = mockk()

            every { request.method } returns "GET"

            shouldThrow<HttpRequestMethodNotSupportedException> {
                usernamePasswordAuthFilter.attemptAuthentication(request, response)
            }

        }
    }


}
