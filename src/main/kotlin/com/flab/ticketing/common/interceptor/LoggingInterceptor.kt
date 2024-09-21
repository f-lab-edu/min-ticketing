package com.flab.ticketing.common.interceptor

import com.flab.ticketing.common.dto.service.trace.TraceId
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Instant
import java.time.temporal.ChronoUnit


@Component
class LoggingInterceptor(
    private val traceId: TraceId

) : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(LoggingInterceptor::class.java)
    private val requestStartTime = "requestStartTime"

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val startTime = Instant.now()
        request.setAttribute(requestStartTime, startTime)

        log.info("[{}] {} {} requested", traceId.id, request.method, request.requestURI)

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
        
        log.info(
            "[{}] {} {} responsed {} - {}ms",
            traceId.id,
            request.method,
            request.requestURI,
            response.status,
            totalTimeMs
        )

    }
}