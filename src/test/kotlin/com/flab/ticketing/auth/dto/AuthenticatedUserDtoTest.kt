package com.flab.ticketing.auth.dto

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.testutils.UnitTest
import io.kotest.matchers.equals.shouldBeEqual

class AuthenticatedUserDtoTest : UnitTest() {

    init {
        "AuthenticatedUserDto 객체를 CustomUserDetails로 부터 필요한 객체를 추출할 수 있다." {
            val uid = "uid"
            val email = "email@email.com"
            val userPW = "12321421asdafd@"
            val nickname = "nickname"

            val customUserDetailsDto = CustomUserDetailsDto(uid, email, userPW, nickname)

            val actual = AuthenticatedUserDto.of(customUserDetailsDto)

            actual.uid shouldBeEqual uid
            actual.email shouldBeEqual email
            actual.nickname shouldBeEqual nickname
        }
    }


}