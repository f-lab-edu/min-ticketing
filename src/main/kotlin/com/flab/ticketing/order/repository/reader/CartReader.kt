package com.flab.ticketing.order.repository.reader

import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.repository.CartRepository
import org.springframework.stereotype.Component


@Component
class CartReader(
    private val cartRepository: CartRepository
) {

    fun findByUser(userUid: String): List<Cart> {
        return cartRepository.findByUserUid(userUid)
    }
}