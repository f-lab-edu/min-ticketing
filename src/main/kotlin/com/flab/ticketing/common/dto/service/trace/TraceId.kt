package com.flab.ticketing.common.dto.service.trace

import com.flab.ticketing.common.utils.NanoIdGenerator
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.stereotype.Component


@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
data class TraceId(
    val id: String = createId()
) {
    private var level = 0

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
        if (level == 0) {
            return "|"
        }

        val sb = StringBuilder().append("|")

        for (i in 0..<level) {
            sb.append("-")
        }


        return sb.append(">").toString()
    }


    fun getEndPrefix(): String {
        if (level == 0) {
            return "|"
        }
        val sb = StringBuilder().append("|").append("<")

        for (i in 0..<level) {
            sb.append("-")
        }

        return sb.toString()
    }

    fun getExceptionPrefix(): String {
        if (level == 0) {
            return "|"
        }
        val sb = StringBuilder().append("|").append("<").append("X")

        for (i in 0..<level) {
            sb.append("-")
        }

        return sb.toString()
    }

}