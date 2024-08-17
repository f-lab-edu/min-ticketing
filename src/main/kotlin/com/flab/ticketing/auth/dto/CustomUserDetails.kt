package com.flab.ticketing.auth.dto

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
    val uid: String,
    private val email: String,
    private val password: String,
    val nickname: String,
    private val authorities: MutableCollection<out GrantedAuthority> = mutableListOf()
) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return authorities
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return email
    }
}