package com.flab.ticketing.common.dto.service.trace

import com.flab.ticketing.common.utils.NanoIdGenerator


data class TraceId(
    val id: String = createId()
) {
    var level = 0
        private set

    companion object {
        private fun createId(): String {
            return NanoIdGenerator.createNanoId(8)
        }
    }


    fun addLevel() {
        this.level++
    }

    fun minusLevel() {
        this.level--
    }

    fun getStartPrefix(): String {
        return "|".repeat(level + 1) + "->"
    }

    fun getEndPrefix(): String {
        return "|".repeat(level + 1) + "<-"
    }

    fun getExceptionPrefix(): String {
        return "|".repeat(level + 1) + "<X-"
    }

}