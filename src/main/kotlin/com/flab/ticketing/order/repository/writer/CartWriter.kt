package com.flab.ticketing.order.repository.writer

import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.order.repository.proxy.ReservationCheck
import com.flab.ticketing.order.repository.proxy.ReservationRelease
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Transactional
@Component
@Logging
class CartWriter(
    private val cartRepository: CartRepository
) {


    @ReservationCheck(
        key = "'cart_save_' + #cart.seat.uid + '_' + #cart.performanceDateTime.uid",
        value = "#cart.user.uid"
    )
    fun save(cart: Cart) {
        cartRepository.save(cart)
    }

    @ReservationRelease(
        key = "'cart_save_' + #cart.seat.uid + '_' + #cart.performanceDateTime.uid",
    )
    fun deleteAll(carts: List<Cart>) {
        cartRepository.deleteAll(carts)
    }
}