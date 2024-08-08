package com.flab.ticketing.auth.filter

import com.flab.ticketing.auth.utils.JwtTokenProvider
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
        val accessToken = request.getHeader(HttpHeaders.AUTHORIZATION)

        val token = accessToken.split("Bearer ")[1]

        val authentication = jwtTokenProvider.getAuthentication(token)

        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)

    }
}