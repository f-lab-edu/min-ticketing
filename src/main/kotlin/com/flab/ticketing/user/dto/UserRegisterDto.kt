package com.flab.ticketing.user.dto

import jakarta.validation.constraints.Email

data class UserRegisterDto(
    @field:Email
    val email: String,
    val password: String,
    val passwordConfirm: String,
    val nickname: String
)