package com.flab.ticketing.user.controller

import com.flab.ticketing.user.dto.UserEmailRegisterDto
import com.flab.ticketing.user.service.EmailService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping


@Controller
@RequestMapping("/api/user")
class UserController(
    private val emailService: EmailService
){

    @PostMapping("/new")
    fun register(@RequestBody emailInfo : UserEmailRegisterDto){
        emailService.sendEmail(emailInfo.email)
    }

}