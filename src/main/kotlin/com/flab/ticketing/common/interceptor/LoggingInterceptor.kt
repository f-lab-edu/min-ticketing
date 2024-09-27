package com.flab.ticketing.common.interceptor

import com.flab.ticketing.common.utils.TraceIdHolder
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Instant
import java.time.temporal.ChronoUnit


@Component
class LoggingInterceptor : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(LoggingInterceptor::class.java)
    private val requestStartTime = "requestStartTime"

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val startTime = Instant.now()
        request.setAttribute(requestStartTime, startTime)

        val traceId = TraceIdHolder.get() ?: TraceIdHolder.set()
        traceId.addLevel()
        log.info("[{}]{} {} {} requested", traceId.id, traceId.getStartPrefix(), request.method, request.requestURI)

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val requestTime = request.getAttribute(requestStartTime) as Instant
        val totalTimeMs = ChronoUnit.MILLIS.between(requestTime, Instant.now())
        val traceId = TraceIdHolder.get() ?: TraceIdHolder.set()
        val prefix = if (ex == null) traceId.getEndPrefix() else traceId.getExceptionPrefix()

        log.info(
            "[{}]{} {} {} responsed {} - {}ms",
            traceId.id,
            prefix,
            request.method,
            request.requestURI,
            response.status,
            totalTimeMs
        )

        TraceIdHolder.release()
    }
}