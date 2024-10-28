package com.flab.ticketing.order.repository.writer

import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.OrderMetaData
import com.flab.ticketing.order.repository.OrderMetaDataRepository
import com.flab.ticketing.order.repository.OrderRepository
import org.springframework.stereotype.Component


@Component
@Logging
class OrderWriter(
    private val orderRepository: OrderRepository,
    private val orderMetaDataRepository: OrderMetaDataRepository
) {

    fun save(order: Order) {
        orderRepository.save(order)
    }

    fun save(orderMetaData: OrderMetaData) {
        orderMetaDataRepository.save(orderMetaData)
    }

    fun delete(order: Order) {
        orderRepository.delete(order)
    }
}