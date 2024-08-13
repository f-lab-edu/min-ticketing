package com.flab.ticketing.auth.filter

import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.common.exception.BusinessException
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.InternalServerException
import com.flab.ticketing.common.exception.UnAuthorizedException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
class JwtAuthenticateFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        runCatching {

            val token = getAccessToken(request)

            if (token == null) {
                filterChain.doFilter(request, response)
                return
            }

            val authentication = jwtTokenProvider.getAuthentication(token)

            SecurityContextHolder.getContext().authentication = authentication

            filterChain.doFilter(request, response)
        }.onFailure { e ->
            when (e) {
                is ExpiredJwtException -> throw UnAuthorizedException(AuthErrorInfos.AUTH_INFO_EXPIRED)
                is JwtException -> throw UnAuthorizedException(AuthErrorInfos.AUTH_INFO_INVALID)
                is BusinessException -> throw e
                else -> throw InternalServerException(CommonErrorInfos.SERVICE_ERROR)
            }
        }

    }

    private fun getAccessToken(request: HttpServletRequest): String? {
        val accessToken = request.getHeader(HttpHeaders.AUTHORIZATION)

        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            return null
        }

        return accessToken.split("Bearer ")[1]
    }
}