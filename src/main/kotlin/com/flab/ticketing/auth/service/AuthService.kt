package com.flab.ticketing.auth.service

import com.flab.ticketing.auth.dto.UserRegisterDto
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.auth.repository.UserRepository
import com.flab.ticketing.auth.utils.EmailCodeGenerator
import com.flab.ticketing.auth.utils.EmailSender
import com.flab.ticketing.auth.utils.EmailVerifier
import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.user.entity.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class AuthService(
    private val emailCodeGenerator: EmailCodeGenerator,
    private val emailSender: EmailSender,
    private val emailVerifier: EmailVerifier,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val nanoIdGenerator: NanoIdGenerator
) {

    @Transactional(readOnly = true)
    fun sendEmailVerifyCode(email: String) {
        val user = userRepository.findByEmail(email)

        if (user != null) {
            throw DuplicatedException(AuthErrorInfos.DUPLICATED_EMAIL)
        }

        val code = emailCodeGenerator.createEmailCode()
        emailVerifier.saveCode(email = email, code = code)
        emailSender.sendEmail(email, "Min-Ticketing 인증 이메일", "MinTicketing 이메일 인증 코드는 $code 입니다.")

    }

    fun verifyEmailCode(email: String, code: String) {
        val savedCode = emailVerifier.getCode(email)

        if (savedCode == null) {
            throw NotFoundException(AuthErrorInfos.EMAIL_VERIFY_INFO_NOT_FOUND)
        }

        if (!savedCode.equals(code)) {
            throw InvalidValueException(AuthErrorInfos.EMAIL_VERIFYCODE_INVALID)
        }

        emailVerifier.setVerifySuccess(email)
    }


    @Transactional
    fun saveVerifiedUserInfo(registerInfo: UserRegisterDto) {
        emailVerifier.checkVerified(registerInfo.email)

        val uid = nanoIdGenerator.createNanoId()
        val encodedPassword = passwordEncoder.encode(registerInfo.password)

        val user = User(uid, registerInfo.email, encodedPassword, registerInfo.nickname)

        userRepository.save(user)
    }

}