package com.flab.ticketing.order.repository.writer

import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.repository.OrderRepository
import org.springframework.stereotype.Component


@Component
@Logging
class OrderWriter(
    private val orderRepository: OrderRepository
) {

    fun save(order: Order) {
        orderRepository.save(order)
    }

    fun delete(order: Order) {
        orderRepository.delete(order)
    }
}