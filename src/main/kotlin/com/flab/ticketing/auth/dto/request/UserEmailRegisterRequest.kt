package com.flab.ticketing.auth.dto.request

import jakarta.validation.constraints.Email

data class UserEmailRegisterRequest(
    @field:Email val email: String
)