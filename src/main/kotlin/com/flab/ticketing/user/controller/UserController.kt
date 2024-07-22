package com.flab.ticketing.user.controller

import com.flab.ticketing.user.dto.UserEmailRegisterDto
import com.flab.ticketing.user.exception.InvalidEmailException
import com.flab.ticketing.user.service.UserService
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService
){

    @Throws(InvalidEmailException::class)
    @PostMapping("/new/email")
    fun emailSend(@Validated @RequestBody emailInfo : UserEmailRegisterDto, bindingResult: BindingResult){
        if(bindingResult.hasFieldErrors("email")){
            throw InvalidEmailException()
        }


        userService.sendEmailVerifyCode(emailInfo.email)
    }




}