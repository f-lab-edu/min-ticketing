package com.flab.ticketing.common.aop.aspect

import com.flab.ticketing.common.dto.service.trace.TraceId
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.text.DecimalFormat


@Aspect
@Component
class LoggingAspect(
    private val traceId: TraceId,
) {

    private val log = LoggerFactory.getLogger(LoggingAspect::class.java)
    private val decimalFormat = DecimalFormat("#,##0.000")


    @Around("@within(com.flab.ticketing.common.aop.Logging) || @annotation(com.flab.ticketing.common.aop.Logging)")
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any {
        val methodSignature = joinPoint.signature as MethodSignature
        val className = joinPoint.target.javaClass.simpleName
        val methodName = methodSignature.name

        traceId.addLevel()
        log.info("[{}]{} {}.{}", traceId.id, traceId.getStartPrefix(), className, methodName)
        val startTimeNs = System.nanoTime()

        try {
            val proceed = joinPoint.proceed()
            val executionTimeMs = (System.nanoTime() - startTimeNs) / 1_000_000.0
            val formattedTime = decimalFormat.format(executionTimeMs)

            log.info(
                "[{}]{} {}.{} ended at {}",
                traceId.id,
                traceId.getEndPrefix(),
                className,
                methodName,
                formattedTime
            )

            return proceed
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