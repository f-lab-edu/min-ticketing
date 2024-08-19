package com.flab.ticketing.auth.utils

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.*
import java.util.stream.Collectors


@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val _secretKey: String,
    @Value("\${jwt.access-token.time}") private val accessTokenTime: Long
) {

    private val secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(_secretKey))

    companion object {
        private const val EMAIL_CLAIM_NAME = "email"
        private const val NICKNAME_CLAIM_NAME = "nickname"
        private const val AUTHORITIES_CLAIM_NAME = "auth"
    }

    fun sign(
        userInfo: AuthenticatedUserDto,
        authorities: MutableCollection<out GrantedAuthority>,
        createTime: Date = Date()
    ): String {

        val authorityString = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","))

        val expiredTime = Date(createTime.time + accessTokenTime)

        return Jwts.builder()
            .subject(userInfo.uid)
            .claim(EMAIL_CLAIM_NAME, userInfo.email)
            .claim(NICKNAME_CLAIM_NAME, userInfo.nickname)
            .claim(AUTHORITIES_CLAIM_NAME, authorityString)
            .expiration(expiredTime)
            .signWith(secretKey)
            .compact()
    }


    fun getAuthentication(accessToken: String): Authentication {
        val claims = parseClaims(accessToken)

        val uid = claims.subject
        val email = claims.get(EMAIL_CLAIM_NAME).toString()
        val nickname = claims.get(NICKNAME_CLAIM_NAME).toString()

        val authorities: MutableCollection<out GrantedAuthority> = claims.get("auth").toString().split(",")
            .mapNotNull { role -> role.takeIf { it.isNotBlank() }?.let { SimpleGrantedAuthority(it) } }
            .toMutableList()

        return UsernamePasswordAuthenticationToken(AuthenticatedUserDto(uid, email, nickname), "", authorities)
    }

    private fun parseClaims(accessToken: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(accessToken)
            .payload
    }


}