package com.flab.ticketing.order.repository

import com.flab.ticketing.order.entity.Cart
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CartRepository : CrudRepository<Cart, Long>