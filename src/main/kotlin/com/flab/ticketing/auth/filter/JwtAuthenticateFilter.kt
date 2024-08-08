package com.flab.ticketing.auth.filter

import com.flab.ticketing.auth.exception.UserErrorInfos
import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.InternalServerException
import com.flab.ticketing.common.exception.UnAuthorizedException
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

            val authentication = jwtTokenProvider.getAuthentication(token)

            SecurityContextHolder.getContext().authentication = authentication

            filterChain.doFilter(request, response)
        }.onFailure { e ->
            when (e) {
                is NullPointerException -> throw UnAuthorizedException(UserErrorInfos.AUTH_INFO_NOT_FOUND)
                else -> throw InternalServerException(CommonErrorInfos.SERVICE_ERROR)
            }
        }

    }

    private fun getAccessToken(request: HttpServletRequest): String {
        val accessToken = request.getHeader(HttpHeaders.AUTHORIZATION)

        return accessToken.split("Bearer ")[1]
    }
}