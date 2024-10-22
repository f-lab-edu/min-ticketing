package com.flab.ticketing.order.repository.writer

import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.exception.InternalServerException
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.exception.OrderErrorInfos
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component


@Aspect
@Component
class CartLockingProxy(
    private val redisTemplate: RedisTemplate<String, String>

) {
    private val LOCK_PREFIX = "cart_lock:"
    private val LOCK_TIMEOUT_MILLIS = 30L * 60_000L // 30분
    private val acquireLockScript: RedisScript<Boolean> = setUpLockScript()


    @Around("execution(* com.flab.ticketing.order.repository.writer.CartWriter.save(..))")
    fun acquireLockBeforeSave(joinPoint: ProceedingJoinPoint): Any? {
        val (key, value) = joinPoint.extractKVFromCart()

        acquireLockOrThrows(key, value)

        return joinPoint.proceed()
    }


    @Around("execution(* com.flab.ticketing.order.repository.writer.CartWriter.deleteAll(..))")
    fun releaseLockBeforeRemove(joinPoint: ProceedingJoinPoint): Any? {
        val keyList = joinPoint.extractKeyListFromCartList()

        redisTemplate.delete(keyList)
        return joinPoint.proceed()
    }


    private fun setUpLockScript(): RedisScript<Boolean> {
        val ACQUIRE_LOCK_SCRIPT = """
            if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then
                redis.call('pexpire', KEYS[1], ARGV[2])
                return 1
            else
                return 0
            end
        """

        return RedisScript.of(ACQUIRE_LOCK_SCRIPT, Boolean::class.java)
    }

    private fun ProceedingJoinPoint.extractKVFromCart(): Pair<String, String> {
        val cart: Cart = this.args
            .firstOrNull { it is Cart }
            ?.let { it as Cart } ?: throw InternalServerException(CommonErrorInfos.SERVICE_ERROR)

        val key = "${LOCK_PREFIX}${cart.performanceDateTime.uid}_${cart.seat.uid}"
        val value = cart.user.uid

        return Pair(key, value)
    }

    private fun ProceedingJoinPoint.extractKeyListFromCartList(): List<String> {
        val carts: List<Cart> = this.args
            .firstOrNull { it is List<*> && it.all { item -> item is Cart } }
            ?.let { it as List<Cart> } ?: throw InternalServerException(CommonErrorInfos.SERVICE_ERROR)

        return carts.map { cart ->
            "${LOCK_PREFIX}${cart.performanceDateTime.uid}_${cart.seat.uid}"
        }
    }

    private fun acquireLockOrThrows(key: String, value: String) {
        val result = redisTemplate.execute(
            acquireLockScript,
            listOf(key),    // KEYS[1]
            value,          // ARGV[1]
            LOCK_TIMEOUT_MILLIS.toString()   // ARGV[2]
        )

        if (!result) {
            throw DuplicatedException(OrderErrorInfos.ALREADY_RESERVED)
        }
    }


}