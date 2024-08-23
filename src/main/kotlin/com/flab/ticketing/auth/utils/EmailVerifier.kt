package com.flab.ticketing.auth.utils

import com.flab.ticketing.auth.entity.EmailVerifyInfo
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.auth.repository.EmailVerifyInfoRepository
import com.flab.ticketing.common.exception.BusinessIllegalStateException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.exception.NotFoundException
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class EmailVerifier(
    private val emailVerifyInfoRepository: EmailVerifyInfoRepository
) {


    fun saveCode(email: String, code: String) {
        emailVerifyInfoRepository.save(EmailVerifyInfo(email, code))
    }

    fun verifyCode(email: String, code: String) {
        val verifyInfo =
            getVerifyCodeEntity(email) ?: throw NotFoundException(AuthErrorInfos.EMAIL_VERIFY_INFO_NOT_FOUND)

        if (verifyInfo.code != code) {
            throw InvalidValueException(AuthErrorInfos.EMAIL_VERIFYCODE_INVALID)
        }

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