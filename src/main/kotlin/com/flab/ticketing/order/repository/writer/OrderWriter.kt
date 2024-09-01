package com.flab.ticketing.order.repository.writer

import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.repository.OrderRepository
import org.springframework.stereotype.Component


@Component
class OrderWriter(
    private val orderRepository: OrderRepository
) {

    fun save(order: Order) {
        orderRepository.save(order)
    }

}