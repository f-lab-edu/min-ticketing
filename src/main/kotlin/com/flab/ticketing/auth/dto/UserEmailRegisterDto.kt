package com.flab.ticketing.auth.dto

import jakarta.validation.constraints.Email

data class UserEmailRegisterDto(
    @field:Email val email: String
)