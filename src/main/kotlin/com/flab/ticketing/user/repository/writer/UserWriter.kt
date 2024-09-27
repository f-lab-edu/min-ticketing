package com.flab.ticketing.user.repository.writer

import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
@Transactional
@Logging
class UserWriter(
    private val userRepository: UserRepository
) {

    fun save(user: User) {
        userRepository.save(user)
    }

}