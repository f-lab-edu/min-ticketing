package com.flab.ticketing.common.utils

import java.util.*


object Base64Utils {
    fun encode(input: String): String {
        return Base64.getEncoder().encodeToString(input.toByteArray())
    }

    fun decode(input: String): String {
        return String(Base64.getDecoder().decode(input))
    }
}