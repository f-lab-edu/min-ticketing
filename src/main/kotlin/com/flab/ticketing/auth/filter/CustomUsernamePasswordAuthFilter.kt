package com.flab.ticketing.auth.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.exception.BadRequestException
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.InternalServerException
import com.flab.ticketing.common.exception.UnAuthorizedException
import com.flab.ticketing.auth.exception.UserErrorInfos
import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.auth.utils.UserLoginInfoConverter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.stereotype.Component
import java.nio.charset.Charset


@Component
class CustomUsernamePasswordAuthFilter(
    authenticationManager: AuthenticationManager,
    private val userLoginInfoConverter: UserLoginInfoConverter,
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper
) :
    AbstractAuthenticationProcessingFilter(LOGIN_URI, authenticationManager) {

    companion object {
        private const val LOGIN_URI = "/api/user/login"
    }


    override fun attemptAuthentication(request: HttpServletRequest?, response: HttpServletResponse?): Authentication {
        if (!request!!.method.equals(HttpMethod.POST.name())) {
            throw BadRequestException(CommonErrorInfos.INVALID_METHOD)
        }

        val charset = request.characterEncoding?.let { Charset.forName(it) } ?: Charset.defaultCharset()
        val (email, password) = userLoginInfoConverter.convert(request.inputStream, charset)

        val authRequest = UsernamePasswordAuthenticationToken(email, password)

        return authenticationManager.authenticate(authRequest)
    }

    override fun successfulAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
        authResult: Authentication
    ) {
        runCatching {
            val userDetails = authResult.principal as UserDetails
            val jwt = jwtTokenProvider.sign(userDetails.username, userDetails.authorities)

            response.status = HttpServletResponse.SC_OK
            response.addHeader(HttpHeaders.AUTHORIZATION, jwt)
        }.onFailure {
            throw InternalServerException(CommonErrorInfos.SERVICE_ERROR)
        }
    }

    override fun unsuccessfulAuthentication(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        failed: AuthenticationException?
    ) {
        throw UnAuthorizedException(UserErrorInfos.LOGIN_FAILED)
    }
}