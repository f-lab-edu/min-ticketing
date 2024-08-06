package com.flab.ticketing.user.service

import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.UserRepository
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
            val password = "asdasf09120312@$@!$@$14"

            every { userRepository.findByEmail(email) } returns createUser(email, password)

            val userDetails = userDetailService.loadUserByUsername(email)

            userDetails.username shouldBe email
            userDetails.password shouldBe password
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
