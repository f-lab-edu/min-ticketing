package com.flab.ticketing.user.utils

import org.springframework.stereotype.Component

@Component
class EmailCodeGenerator {

    fun createEmailCode() : String{
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZ0123456789"
        return (1..6)
            .map { charset.random() }
            .joinToString("")

    }

}