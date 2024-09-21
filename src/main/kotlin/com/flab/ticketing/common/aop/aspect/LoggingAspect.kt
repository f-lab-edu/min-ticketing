package com.flab.ticketing.common.aop.aspect

import com.flab.ticketing.common.dto.service.trace.TraceId
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit


@Aspect
@Component
class LoggingAspect(
    private val traceId: TraceId,
) {

    private val log = LoggerFactory.getLogger(LoggingAspect::class.java)

    @Around("@within(com.flab.ticketing.common.aop.Logging) || @annotation(com.flab.ticketing.common.aop.Logging)")
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val methodSignature = joinPoint.signature as MethodSignature
        val className = joinPoint.target.javaClass.simpleName
        val methodName = methodSignature.name

        traceId.addLevel()
        log.info("[{}]{} {}.{}", traceId.id, traceId.getStartPrefix(), className, methodName)
        val startTime = Instant.now()

        return try {
            val proceed = joinPoint.proceed()
            val executionTimeMs = ChronoUnit.MILLIS.between(startTime, Instant.now())

            log.info(
                "[{}]{} {}.{} ended at {}ms",
                traceId.id,
                traceId.getEndPrefix(),
                className,
                methodName,
                executionTimeMs
            )

            proceed
        } catch (e: Exception) {
            log.info(
                "[{}]{} {}.{} ended by exception {}",
                traceId.id,
                traceId.getExceptionPrefix(),
                className,
                methodName,
                e.javaClass.simpleName
            )
            throw e
        } finally {
            traceId.minusLevel()
        }

    }


}