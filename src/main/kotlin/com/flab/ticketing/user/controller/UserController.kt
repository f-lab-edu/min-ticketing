package com.flab.ticketing.user.controller

import com.flab.ticketing.user.dto.UserEmailRegisterDto
import com.flab.ticketing.user.service.UserService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService
){

    @PostMapping("/new/email")
    fun emailSend(@RequestBody emailInfo : UserEmailRegisterDto){
        userService.sendEmailVerifyCode(emailInfo.email)
    }




}