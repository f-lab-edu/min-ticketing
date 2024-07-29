package com.flab.ticketing.user.dto

import jakarta.validation.constraints.Email

data class UserEmailRegisterDto(
    @field:Email val email: String
)