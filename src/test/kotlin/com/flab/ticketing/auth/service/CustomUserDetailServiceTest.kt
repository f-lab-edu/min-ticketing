package com.flab.ticketing.auth.service

import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.common.exception.UnAuthorizedException
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.reader.UserReader
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

class CustomUserDetailServiceTest : UnitTest() {

    private val userReader: UserReader = mockk()
    private val userDetailService: UserDetailsService = CustomUserDetailService(userReader)

    init {
        "사용자의 Email 정보를 읽어서 UserDetail을 반환할 수 있다." {
            val email = "email@email.com"
            val userPW = "asdasf09120312@$@!$@$14"
            val uid = "uid"
            val nickname = "nickname"

            every { userReader.findByEmail(email) } returns createUser(email, userPW, uid, nickname)

            val userDetails = userDetailService.loadUserByUsername(email) as? CustomUserDetailsDto
                ?: fail("리턴 타입은 CustomUserDetail 구현체 여야 합니다.")

            userDetails.uid shouldBe uid
            userDetails.username shouldBe email
            userDetails.password shouldBe userPW
            userDetails.nickname shouldBe nickname
        }

        "사용자의 Email 정보를 읽을 수 없으면 UnAuthorizedException을 반환한다." {
            val email = "email@email.com"

            every { userReader.findByEmail(email) } throws UnAuthorizedException(AuthErrorInfos.USER_INFO_NOT_FOUND)

            shouldThrow<UsernameNotFoundException> {
                userDetailService.loadUserByUsername(email)
            }

        }

    }


    private fun createUser(
        email: String,
        password: String,
        uid: String = "notUsed",
        nickname: String = "notUsed"
    ): User {
        return User(uid, email, password, nickname)
    }

}