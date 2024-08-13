package com.flab.ticketing.auth.utils

import com.flab.ticketing.auth.entity.EmailVerifyInfo
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.auth.repository.EmailVerifyInfoRepository
import com.flab.ticketing.common.exception.BusinessIllegalStateException
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
        val verifyCodeInfo = getVerifyCodeEntity(email)

        if (verifyCodeInfo == null) {
            throw BusinessIllegalStateException(AuthErrorInfos.EMAIL_VERIFY_INFO_NOT_FOUND)
        }
        if (!verifyCodeInfo.isVerified) {
            throw BusinessIllegalStateException(AuthErrorInfos.EMAIL_NOT_VERIFIED)
        }
    }
}