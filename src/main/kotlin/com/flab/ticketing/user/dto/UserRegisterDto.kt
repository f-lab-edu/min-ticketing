package com.flab.ticketing.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

data class UserRegisterDto(
    @field:Email
    val email: String,

    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#\$%^&*])[A-Za-z0-9!@#\$%^&*]{8,}$"
    )
    val password: String,
    val passwordConfirm: String,
    val nickname: String
)