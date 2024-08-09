package com.flab.ticketing.auth.service

import com.flab.ticketing.auth.repository.UserRepository
import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.user.entity.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

class CustomUserDetailServiceTest : UnitTest() {

    private val userRepository: UserRepository = mockk()
    private val userDetailService: UserDetailsService = CustomUserDetailService(userRepository)

    init {
        "사용자의 Email 정보를 읽어서 UserDetail을 반환할 수 있다." {
            val email = "email@email.com"
            val userPW = "asdasf09120312@$@!$@$14"

            every { userRepository.findByEmail(email) } returns createUser(email, userPW)

            val userDetails = userDetailService.loadUserByUsername(email)

            userDetails.username shouldBe email
            userDetails.password shouldBe userPW
        }

        "사용자의 Email 정보를 읽을 수 없으면 UnAuthorizedException을 반환한다." {
            val email = "email@email.com"

            every { userRepository.findByEmail(email) } returns null

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
