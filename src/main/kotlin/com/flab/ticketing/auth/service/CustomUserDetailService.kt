package com.flab.ticketing.auth.service

import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.user.repository.reader.UserReader
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
class CustomUserDetailService(private val userReader: UserReader) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String?): UserDetails {
        if (username == null) {
            throw UsernameNotFoundException(AuthErrorInfos.USER_INFO_NOT_FOUND.message)
        }

        runCatching {
            val user = userReader.findByEmail(username)

            return CustomUserDetailsDto(user.uid, user.email, user.password, user.nickname)
        }.getOrElse {
            throw UsernameNotFoundException(AuthErrorInfos.USER_INFO_NOT_FOUND.message)
        }
    }
}