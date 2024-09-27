package com.flab.ticketing.auth.dto.service

data class AuthenticatedUserDto(
    val uid: String,
    val email: String,
    val nickname: String
) {
    companion object {
        fun of(customUserDetailsDto: CustomUserDetailsDto): AuthenticatedUserDto {
            return AuthenticatedUserDto(
                customUserDetailsDto.uid,
                customUserDetailsDto.username,
                customUserDetailsDto.nickname
            )
        }

    }


}