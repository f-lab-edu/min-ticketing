package com.flab.ticketing.user.service

import com.flab.ticketing.user.repository.EmailRepository
import com.flab.ticketing.user.utils.EmailCodeGenerator
import com.flab.ticketing.user.utils.EmailSender
import org.springframework.stereotype.Service


@Service
class EmailService(
    private val emailCodeGenerator: EmailCodeGenerator,
    private val emailSender: EmailSender,
    private val emailRepository: EmailRepository
) {


    fun sendEmail(email : String){
        val code = emailCodeGenerator.createEmailCode()
        emailRepository.saveCode(email = email, code=code)
        emailSender.sendEmail(email, "Min-Ticketing 인증 이메일","MinTicketing 이메일 인증 코드는 $code 입니다.")

    }

}