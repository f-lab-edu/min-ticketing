package com.flab.ticketing.auth.service

import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.user.entity.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
class CustomUserDetailService(private val userRepository: UserRepository) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String?): UserDetails {
        if (username == null) {
            throw UsernameNotFoundException("유저 정보를 조회할 수 없습니다.")
        }

        val user =
            userRepository.findByEmail(username) ?: throw UsernameNotFoundException("유저 정보를 조회할 수 없습니다.")

        return CustomUserDetailsDto(user.uid, user.email, user.password, user.nickname)
    }
}