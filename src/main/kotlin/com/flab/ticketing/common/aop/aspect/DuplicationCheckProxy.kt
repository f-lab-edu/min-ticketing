package com.flab.ticketing.common.aop.aspect

import com.flab.ticketing.common.aop.DuplicatedCheck
import com.flab.ticketing.common.aop.utils.CustomSpringELParser.getDynamicValue
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.exception.InternalServerException
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.common.aop.ReleaseDuplicateCheck
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration


@Aspect
@Component
class DuplicationCheckProxy(
    private val redisTemplate: RedisTemplate<String, String>

) {
    private val LOCK_PREFIX = "lock:"
    private val LOCK_TIMEOUT_MILLIS = Duration.ofMillis(30L * 60_000L) // 30분


    @Around("@annotation(com.flab.ticketing.common.aop.DuplicatedCheck)")
    fun acquireLock(joinPoint: ProceedingJoinPoint): Any? {
        val (key, value) = extractKeyFromCart(joinPoint)
        acquireLockOrThrows(key, value)

        return joinPoint.proceed()
    }


    @Around("@annotation(com.flab.ticketing.common.aop.ReleaseDuplicateCheck)")
    fun releaseLock(joinPoint: ProceedingJoinPoint): Any? {
        val keyList = extractReleaseKeys(joinPoint)

        redisTemplate.delete(keyList)
        return joinPoint.proceed()
    }

    private fun acquireLockOrThrows(key: String, value: String) {
        val result = redisTemplate.opsForValue()
            .setIfAbsent(key, value, LOCK_TIMEOUT_MILLIS)!!

        if (!result) {
            throw DuplicatedException(OrderErrorInfos.ALREADY_RESERVED)
        }
    }


    private fun extractKeyFromCart(joinPoint: ProceedingJoinPoint): Pair<String, String> {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(DuplicatedCheck::class.java)


        val key = "${LOCK_PREFIX}${getDynamicValue(signature.parameterNames, joinPoint.args, annotation.key)}"
        val value = "${Thread.currentThread().id}:${System.currentTimeMillis()}"

        return Pair(key, value)
    }

    private fun extractReleaseKeys(joinPoint: ProceedingJoinPoint): List<String> {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(ReleaseDuplicateCheck::class.java)
        val parameterNames = signature.parameterNames


        return when (val result = getDynamicValue(parameterNames, joinPoint.args, annotation.key)) {
            is List<*> -> result.filterNotNull().map { "${LOCK_PREFIX}$it" }
            null -> throw InternalServerException(CommonErrorInfos.SERVICE_ERROR)
            else -> listOf(
                "${LOCK_PREFIX}$result"
            )
        }
    }


}