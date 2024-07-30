package com.flab.ticketing.user.repository

import com.flab.ticketing.user.entity.EmailVerifyInfo
import org.springframework.stereotype.Component

@Component
class EmailRepository(
    private val emailVerifyInfoRepository: EmailVerifyInfoRepository
) {


    fun saveCode(email: String, code: String) {
        emailVerifyInfoRepository.save(EmailVerifyInfo(email, code))
    }

    fun getCode(email: String): String? {
        val verifyInfoOptional = emailVerifyInfoRepository.findById(email)
        if (verifyInfoOptional.isEmpty) {
            return null
        }

        return verifyInfoOptional.get().code
    }
}