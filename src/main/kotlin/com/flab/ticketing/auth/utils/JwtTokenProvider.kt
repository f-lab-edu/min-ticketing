package com.flab.ticketing.auth.utils

import com.flab.ticketing.auth.dto.CustomUserDetails
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
        private const val CLAIM_NAME = "auth"
    }

    fun sign(id: String, authorities: MutableCollection<out GrantedAuthority>, createTime: Date = Date()): String {
        val authorityString = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","))

        val expiredTime = Date(createTime.time + accessTokenTime)

        return Jwts.builder()
            .subject(id)
            .claim(CLAIM_NAME, authorityString)
            .expiration(expiredTime)
            .signWith(secretKey)
            .compact()
    }

    fun getAuthentication(accessToken: String): Authentication {
        val claims = parseClaims(accessToken)

        val email = claims.subject

        val authorities: MutableCollection<out GrantedAuthority> = claims.get("auth").toString().split(",")
            .mapNotNull { role -> role.takeIf { it.isNotBlank() }?.let { SimpleGrantedAuthority(it) } }
            .toMutableList()

        return UsernamePasswordAuthenticationToken(CustomUserDetails(email, ""), "", authorities)
    }

    private fun parseClaims(accessToken: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(accessToken)
            .payload
    }


}