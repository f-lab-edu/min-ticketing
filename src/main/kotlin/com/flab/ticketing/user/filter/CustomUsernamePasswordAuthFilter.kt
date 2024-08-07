package com.flab.ticketing.user.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.ErrorInfo
import com.flab.ticketing.user.exception.UserErrorInfos
import com.flab.ticketing.user.utils.JwtTokenProvider
import com.flab.ticketing.user.utils.UserLoginInfoConverter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.stereotype.Component
import org.springframework.web.HttpRequestMethodNotSupportedException
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
            throw HttpRequestMethodNotSupportedException("로그인은 POST로 호출되어야 합니다.")
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
        try {
            val userDetails = authResult.principal as? UserDetails
                ?: return sendError(response, CommonErrorInfos.SERVICE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)

            val jwt = jwtTokenProvider.sign(userDetails.username, userDetails.authorities)

            response.status = HttpServletResponse.SC_OK
            response.addHeader(HttpHeaders.AUTHORIZATION, jwt)
        } catch (e: Exception) {
            sendError(response, UserErrorInfos.LOGIN_FAILED, HttpStatus.UNAUTHORIZED)
        }
    }

    private fun sendError(response: HttpServletResponse, errorInfo: ErrorInfo, status: HttpStatus) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        response.writer.write(objectMapper.writeValueAsString(ErrorResponse.of(errorInfo)))
    }
}