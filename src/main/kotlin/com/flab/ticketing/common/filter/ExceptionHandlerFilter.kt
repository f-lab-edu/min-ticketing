package com.flab.ticketing.common.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.common.exception.BadRequestException
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.ErrorInfo
import com.flab.ticketing.common.exception.UnAuthorizedException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
class ExceptionHandlerFilter(private val objectMapper: ObjectMapper) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        runCatching {
            filterChain.doFilter(request, response)
        }.onFailure { e ->
            when (e) {
                is BadRequestException -> sendBusinessError(response, e.info, HttpStatus.BAD_REQUEST)
                is UnAuthorizedException -> sendBusinessError(response, e.info, HttpStatus.UNAUTHORIZED)
                else -> sendUnknownError(response)
            }
        }
    }

    private fun sendBusinessError(response: HttpServletResponse, errorInfo: ErrorInfo, status: HttpStatus) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        val errorResponse = ErrorResponse.of(errorInfo)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }

    private fun sendUnknownError(response: HttpServletResponse) {
        response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        val errorResponse = ErrorResponse.of(CommonErrorInfos.SERVICE_ERROR)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}