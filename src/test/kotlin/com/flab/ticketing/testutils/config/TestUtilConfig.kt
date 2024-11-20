package com.flab.ticketing.testutils.config

import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.testutils.persistence.UserTestUtils
import com.flab.ticketing.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * TestUtil 클래스를 정의하는 Configuration 파일입니다. Repository는 미리 등록되어 있다고 가정합니다.
 * @author minseok kim
 */
@TestConfiguration
class TestUtilConfig {

    @Bean
    fun jwtTokenProvider(
        @Value("\${jwt.secret:SECRETSECRETSECRETSECRETSECRETSECRETSECRETSECRETSECRET}") secretKey: String,
        @Value("\${jwt.access-token.time:1000000}") accessTokenTime: Long
    ): JwtTokenProvider {
        return JwtTokenProvider(secretKey, accessTokenTime)
    }


    @Bean
    fun testPasswordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun userTestUtils(
        jwtTokenProvider: JwtTokenProvider,
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder
    ): UserTestUtils {
        return UserTestUtils(userRepository, jwtTokenProvider, passwordEncoder)
    }


}