package com.flab.ticketing.user.repository

import com.flab.ticketing.user.entity.EmailVerifyInfo
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class EmailVerifier(
    private val emailVerifyInfoRepository: EmailVerifyInfoRepository
) {


    fun saveCode(email: String, code: String) {
        emailVerifyInfoRepository.save(EmailVerifyInfo(email, code))
    }

    fun getCode(email: String): String? {
        val verifyInfoOptional = getVerifyCodeEntity(email) ?: return null

        return verifyInfoOptional.code
    }

    fun setVerifySuccess(email: String) {
        val verifyInfo = getVerifyCodeEntity(email) ?: return

        verifyInfo.isVerified = true
        emailVerifyInfoRepository.save(verifyInfo)


    }

    private fun getVerifyCodeEntity(email: String): EmailVerifyInfo? {
        val verifyInfoOptional = emailVerifyInfoRepository.findById(email)

        return verifyInfoOptional.getOrNull()
    }

    fun checkVerified(email: String) {
        TODO()
    }
}