package com.flab.ticketing.common.utils

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import java.util.*


object NanoIdGenerator {

    fun createNanoId(nanoIdSize: Int = 10): String {
        val random = Random()
        return NanoIdUtils.randomNanoId(random, NanoIdUtils.DEFAULT_ALPHABET, nanoIdSize)
    }

}