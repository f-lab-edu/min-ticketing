package com.flab.ticketing.auth.controller

import com.flab.ticketing.auth.dto.UserEmailRegisterDto
import com.flab.ticketing.auth.dto.UserEmailVerificationDto
import com.flab.ticketing.auth.dto.UserPasswordUpdateDto
import com.flab.ticketing.auth.dto.UserRegisterDto
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.auth.resolver.annotation.LoginUser
import com.flab.ticketing.auth.service.AuthService
import com.flab.ticketing.common.exception.InvalidValueException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/new/email")
    fun emailSend(@Validated @RequestBody emailInfo: UserEmailRegisterDto) {
        authService.sendEmailVerifyCode(emailInfo.email)
    }


    @PostMapping("/new/email/verify")
    fun verifyEmailCode(@Validated @RequestBody verifyInfo: UserEmailVerificationDto) {
        authService.verifyEmailCode(verifyInfo.email, verifyInfo.code)
    }


    @PostMapping("/new/info")
    fun saveVerifiedUserInfo(@Validated @RequestBody registerInfo: UserRegisterDto) {
        if (!registerInfo.password.equals(registerInfo.passwordConfirm)) {
            throw InvalidValueException(AuthErrorInfos.PASSWORD_CONFIRM_NOT_EQUALS)
        }

        authService.saveVerifiedUserInfo(registerInfo)
    }

    @PatchMapping("/password")
    fun updatePassword(@LoginUser email: String, @Validated @RequestBody passwordUpdateDto: UserPasswordUpdateDto) {
        if (!passwordUpdateDto.newPassword.equals(passwordUpdateDto.newPasswordConfirm)) {
            throw InvalidValueException(AuthErrorInfos.PASSWORD_CONFIRM_NOT_EQUALS)
        }


        authService.updatePassword(email, passwordUpdateDto.currentPassword, passwordUpdateDto.newPassword)
    }

}