package com.flab.ticketing.auth.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

data class UserEmailVerificationRequest(
    @field:Email val email: String,
    @field:Pattern(regexp = "^[A-Z0-9]{6}\$") val code: String

)