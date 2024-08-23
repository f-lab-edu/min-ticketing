package com.flab.ticketing.order.repository

import com.flab.ticketing.order.entity.Cart
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CartRepository : JpaRepository<Cart, Long> {
    
    fun save(cart: Cart)
    fun findByDateUidAndSeatUid(dateUid: String, seatUid: String): Cart?
}