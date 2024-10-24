package com.flab.ticketing.order.repository.writer

import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.common.aop.DuplicatedCheck
import com.flab.ticketing.common.aop.ReleaseDuplicateCheck
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Transactional
@Component
@Logging
class CartWriter(
    private val cartRepository: CartRepository
) {


    @DuplicatedCheck(
        key = "#cart.seat.uid + '_' + #cart.performanceDateTime.uid",
    )
    fun save(cart: Cart) {
        cartRepository.save(cart)
    }

    @ReleaseDuplicateCheck(
        key = "#carts.![seat.uid + '_' + performanceDateTime.uid]",
    )
    fun deleteAll(carts: List<Cart>) {
        cartRepository.deleteAll(carts)
    }
}