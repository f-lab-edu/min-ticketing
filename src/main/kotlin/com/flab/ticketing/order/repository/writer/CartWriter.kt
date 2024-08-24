package com.flab.ticketing.order.repository.writer

import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.repository.CartRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Transactional
@Component
class CartWriter(
    private val cartRepository: CartRepository
) {

    fun save(cart: Cart) {
        cartRepository.save(cart)
    }
}