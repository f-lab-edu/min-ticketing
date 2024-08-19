package com.flab.ticketing.auth.dto.request

import jakarta.validation.constraints.Pattern

data class UserPasswordUpdateRequest(
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#\$%^&*])[A-Za-z0-9!@#\$%^&*]{8,}$"
    )
    val currentPassword: String,

    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#\$%^&*])[A-Za-z0-9!@#\$%^&*]{8,}$"
    )
    val newPassword: String,

    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#\$%^&*])[A-Za-z0-9!@#\$%^&*]{8,}$"
    )
    val newPasswordConfirm: String

)