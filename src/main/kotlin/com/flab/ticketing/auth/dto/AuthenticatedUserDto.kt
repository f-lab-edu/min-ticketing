package com.flab.ticketing.auth.dto

data class AuthenticatedUserDto(
    val uid: String,
    val email: String,
    val nickname: String
) {
    companion object {
        fun of(customUserDetails: CustomUserDetails): AuthenticatedUserDto {
            return AuthenticatedUserDto(customUserDetails.uid, customUserDetails.username, customUserDetails.nickname)
        }

    }


}