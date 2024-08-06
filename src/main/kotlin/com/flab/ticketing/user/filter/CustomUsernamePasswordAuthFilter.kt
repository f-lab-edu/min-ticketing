package com.flab.ticketing.user.filter

import com.flab.ticketing.user.utils.UserLoginInfoConverter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.web.HttpRequestMethodNotSupportedException
import java.nio.charset.Charset

class CustomUsernamePasswordAuthFilter(
    authenticationManager: AuthenticationManager,
    private val userLoginInfoConverter: UserLoginInfoConverter
) :
    AbstractAuthenticationProcessingFilter(LOGIN_URI, authenticationManager) {

    companion object {
        private const val LOGIN_URI = "/api/user/login"
    }


    override fun attemptAuthentication(request: HttpServletRequest?, response: HttpServletResponse?): Authentication {
        if (!request!!.method.equals("POST")) {
            throw HttpRequestMethodNotSupportedException("로그인은 POST로 호출되어야 합니다.")
        }

        val charset = request.characterEncoding?.let { Charset.forName(it) } ?: Charset.defaultCharset()
        val (email, password) = userLoginInfoConverter.convert(request.inputStream, charset)

        val authRequest = UsernamePasswordAuthenticationToken(email, password)

        return authenticationManager.authenticate(authRequest)
    }
}