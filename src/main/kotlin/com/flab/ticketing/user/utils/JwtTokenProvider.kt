package com.flab.ticketing.user.utils

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Component
import java.util.*
import java.util.stream.Collectors


@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.access-token.time}") private val accessTokenTime: Long
) {

    fun sign(id: String, authorities: MutableCollection<out GrantedAuthority>, createTime: Date = Date()): String {
        val authorityString = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","))

        val expiredTime = Date(createTime.time + accessTokenTime)
        val keyBytes = Decoders.BASE64.decode(secretKey)

        return Jwts.builder()
            .subject(id)
            .claim("auth", authorityString)
            .expiration(expiredTime)
            .signWith(Keys.hmacShaKeyFor(keyBytes))
            .compact()
    }

}