package com.flab.ticketing.auth.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.common.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        authException: AuthenticationException?
    ) {
        response!!.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        val errorResponse = ErrorResponse.of(AuthErrorInfos.AUTH_INFO_INVALID)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}