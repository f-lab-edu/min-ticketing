package com.flab.ticketing.testutils.persistence

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.testutils.fixture.UserFixture
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*


class UserPersistenceUtils(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder
) {


    /**
     * 사용자 객체를 생성하고 DB에 저장 합니다.
     * @author minseok kim
     */
    fun saveNewUser(
        uid: String = "uid1232",
        email: String = "email@email.com",
        rawPassword: String = "enc123Rypt42ed",
        nickname: String = "nickname"
    ): User {
        val user = UserFixture.createUser(
            uid, email, passwordEncoder.encode(rawPassword), nickname
        )

        return userRepository.save(user)
    }

    /**
     * 사용자 객체를 생성하고 DB에 저장, JWT Token을 생성해 반환합니다.
     * @author minseok kim
     * @param createDate JWT 토큰 생성시간, 만료 시간은 ${jwt.access-token.time} 참고
     * @return User, Token 정보가 담긴 Pair
     */
    fun saveUserAndCreateJwt(
        uid: String = "uid1232",
        email: String = "email@email.com",
        rawPassword: String = "enc123Rypt42ed",
        nickname: String = "nickname",
        createDate: Date = Date()
    ): Pair<User, String> {
        val user = UserFixture.createUser(
            uid, email, passwordEncoder.encode(rawPassword), nickname
        )

        userRepository.save(user)
        val jwt = jwtTokenProvider.sign(AuthenticatedUserDto(uid, email, nickname), mutableListOf(), createDate)

        return user to jwt
    }


    /**
     * User Repository 데이터를 모두 제거합니다. 사용할 때 Cart, Order 등 외부 의존이 존재한다면 오류가 발생할 수 있습니다.
     * @author minseok kim
     */
    fun clearContext() {
        userRepository.deleteAll()
    }

}