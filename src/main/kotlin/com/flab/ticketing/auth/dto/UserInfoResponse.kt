package com.flab.ticketing.auth.dto

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto

data class UserInfoResponse(
    val id: String,
    val nickname: String
) {
    companion object {
        fun of(userInfo: AuthenticatedUserDto): UserInfoResponse {
            return UserInfoResponse(
                userInfo.uid,
                userInfo.nickname
            )
        }
    }
}