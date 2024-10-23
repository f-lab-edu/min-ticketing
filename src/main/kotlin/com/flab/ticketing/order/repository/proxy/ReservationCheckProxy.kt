package com.flab.ticketing.order.repository.proxy

import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.exception.InternalServerException
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.exception.OrderErrorInfos
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
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


    @Around("execution(* com.flab.ticketing.order.repository.writer.CartWriter.save(..))")
    fun acquireLock(joinPoint: ProceedingJoinPoint): Any? {
        val (key, value) = extractKVFromCart(joinPoint)
        acquireLockOrThrows(key, value)

        return joinPoint.proceed()
    }


    @Around("execution(* com.flab.ticketing.order.repository.writer.CartWriter.deleteAll(..))")
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
        val cart: Cart = joinPoint.args
            .firstOrNull { it is Cart }
            ?.let { it as Cart } ?: throw InternalServerException(CommonErrorInfos.SERVICE_ERROR)

        val key = "${LOCK_PREFIX}${cart.performanceDateTime.uid}_${cart.seat.uid}"
        val value = cart.user.uid

        return Pair(key, value)
    }

    private fun extractKeyListFromCartList(joinPoint: ProceedingJoinPoint): List<String> {
        val carts: List<Cart> = joinPoint.args
            .firstOrNull { it is List<*> && it.all { item -> item is Cart } }
            ?.let { it as List<Cart> } ?: throw InternalServerException(CommonErrorInfos.SERVICE_ERROR)

        return carts.map { cart ->
            "${LOCK_PREFIX}${cart.performanceDateTime.uid}_${cart.seat.uid}"
        }
    }




}