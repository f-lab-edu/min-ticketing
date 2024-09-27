package com.flab.ticketing.common.utils

import com.flab.ticketing.common.dto.service.trace.TraceId

object TraceIdHolder {

    private val threadLocal = ThreadLocal<TraceId>()

    fun get(): TraceId? {
        return threadLocal.get()
    }

    fun set(traceId: TraceId = TraceId()): TraceId {
        threadLocal.set(traceId)

        return traceId
    }

    fun release() {
        threadLocal.remove()
    }

}