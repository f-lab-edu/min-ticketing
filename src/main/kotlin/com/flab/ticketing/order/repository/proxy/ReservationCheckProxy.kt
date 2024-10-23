package com.flab.ticketing.order.repository.proxy

import com.flab.ticketing.common.aop.utils.CustomSpringELParser
import com.flab.ticketing.common.aop.utils.CustomSpringELParser.getDynamicValue
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.exception.InternalServerException
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.exception.OrderErrorInfos
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration


@Aspect
@Component
class ReservationCheckProxy(
    private val redisTemplate: RedisTemplate<String, String>

) {
    private val LOCK_PREFIX = "lock:"
    private val LOCK_TIMEOUT_MILLIS = Duration.ofMillis(30L * 60_000L) // 30분


    @Around("@annotation(com.flab.ticketing.order.repository.proxy.ReservationCheck)")
    fun acquireLock(joinPoint: ProceedingJoinPoint): Any? {
        val (key, value) = extractKVFromCart(joinPoint)
        acquireLockOrThrows(key, value)

        return joinPoint.proceed()
    }


    @Around("@annotation(com.flab.ticketing.order.repository.proxy.ReservationRelease)")
    fun releaseLock(joinPoint: ProceedingJoinPoint): Any? {
        val keyList = extractKeyListFromCartList(joinPoint)

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


    private fun extractKVFromCart(joinPoint: ProceedingJoinPoint): Pair<String, String> {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(ReservationCheck::class.java)


        val key = "${LOCK_PREFIX}${getDynamicValue(signature.parameterNames, joinPoint.args, annotation.key)}"
        val value = getDynamicValue(signature.parameterNames, joinPoint.args, annotation.value).toString()

        return Pair(key, value)
    }

    private fun extractKeyListFromCartList(joinPoint: ProceedingJoinPoint): List<String> {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(ReservationRelease::class.java)

        val carts = joinPoint.args
            .firstOrNull { it is List<*> && it.all { item -> item is Cart } }
            ?.let { it as List<Cart> }
            ?: throw InternalServerException(CommonErrorInfos.SERVICE_ERROR)

        return carts.map { cart ->
            val key = getDynamicValue(
                arrayOf("cart"),
                arrayOf(cart),
                annotation.key
            )
            "${LOCK_PREFIX}$key"
        }
    }


}