package com.flab.ticketing.auth.dto.request

data class UserLoginRequest(
    val email: String,
    val password: String
)