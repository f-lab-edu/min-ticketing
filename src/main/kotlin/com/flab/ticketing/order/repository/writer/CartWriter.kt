package com.flab.ticketing.order.repository.writer

import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.repository.CartRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Transactional
@Component
@Logging
class CartWriter(
    private val cartRepository: CartRepository
) {

    fun save(cart: Cart) {
        cartRepository.save(cart)
    }

    fun saveAll(carts: List<Cart>) {
        cartRepository.saveAll(carts)
    }

    fun deleteAll(carts: List<Cart>) {
        cartRepository.deleteAll(carts)
    }
}