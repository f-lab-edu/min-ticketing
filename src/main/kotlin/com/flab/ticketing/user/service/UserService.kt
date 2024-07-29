package com.flab.ticketing.user.service

import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.user.exception.UserErrorInfos
import com.flab.ticketing.user.repository.EmailRepository
import com.flab.ticketing.user.repository.UserRepository
import com.flab.ticketing.user.utils.EmailCodeGenerator
import com.flab.ticketing.user.utils.EmailSender
import org.springframework.stereotype.Service


@Service
class UserService(
    private val emailCodeGenerator: EmailCodeGenerator,
    private val emailSender: EmailSender,
    private val emailRepository: EmailRepository,
    private val userRepository: UserRepository
) {

    fun sendEmailVerifyCode(email: String) {
        val user = userRepository.findByEmail(email)

        if (user != null) {
            throw DuplicatedException(UserErrorInfos.DUPLICATED_EMAIL)
        }

        val code = emailCodeGenerator.createEmailCode()
        emailRepository.saveCode(email = email, code = code)
        emailSender.sendEmail(email, "Min-Ticketing 인증 이메일", "MinTicketing 이메일 인증 코드는 $code 입니다.")

    }

    fun verifyEmailCode(email: String, code: String) {
        val savedCode = emailRepository.getCode(email)

        if (savedCode == null) {
            throw NotFoundException(UserErrorInfos.EMAIL_VERIFYCODE_NOT_FOUND)
        }

        if (!savedCode.equals(code)) {
            throw InvalidValueException(UserErrorInfos.EMAIL_VERIFYCODE_INVALID)
        }

    }

}