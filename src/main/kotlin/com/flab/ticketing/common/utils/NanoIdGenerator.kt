package com.flab.ticketing.common.utils

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import org.springframework.stereotype.Component
import java.util.*


@Component
class NanoIdGenerator {

    private val nanoIdSize = 10

    fun createNanoId(): String {
        val random = Random()
        return NanoIdUtils.randomNanoId(random, NanoIdUtils.DEFAULT_ALPHABET, nanoIdSize)
    }

}