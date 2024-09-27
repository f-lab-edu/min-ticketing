package com.flab.ticketing.user.repository.reader

import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.common.exception.UnAuthorizedException
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
@Transactional(readOnly = true)
@Logging
class UserReader(
    private val userRepository: UserRepository
) {

    fun findByEmail(email: String): User {
        return userRepository.findByEmail(email) ?: throw UnAuthorizedException(AuthErrorInfos.USER_INFO_NOT_FOUND)
    }

    fun isEmailExists(email: String): Boolean {
        return userRepository.findByEmail(email)?.let { true } ?: false
    }

    fun findByUid(userUid: String): User {
        return userRepository.findByUid(userUid) ?: throw UnAuthorizedException(AuthErrorInfos.USER_INFO_NOT_FOUND)
    }
}