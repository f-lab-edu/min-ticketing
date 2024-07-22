package com.flab.ticketing.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

data class UserEmailVerificationDto(
    @field:Email val email: String,
    @field:Pattern(regexp = "^[A-Z0-9]{6}\$") val code : String

)