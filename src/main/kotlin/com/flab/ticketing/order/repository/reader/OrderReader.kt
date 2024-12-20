package com.flab.ticketing.order.repository.reader

import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.order.dto.request.OrderSearchConditions
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.OrderMetaData
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.OrderMetaDataRepository
import com.flab.ticketing.order.repository.OrderRepository
import org.springframework.stereotype.Component

@Component
@Logging
class OrderReader(
    private val orderRepository: OrderRepository,
    private val orderMetaDataRepository: OrderMetaDataRepository
) {

    fun findByUid(uid: String): Order {
        return orderRepository.findByUid(uid) ?: throw NotFoundException(OrderErrorInfos.ORDER_INFO_NOT_FOUND)
    }

    fun findOrderByUser(
        userUid: String,
        searchConditions: OrderSearchConditions,
        cursorInfo: CursorInfoDto
    ): List<Order> {
        return orderRepository.findByUser(userUid, cursorInfo, searchConditions).filterNotNull()
    }

    fun findMetaData(
        orderId: String
    ): OrderMetaData {
        return orderMetaDataRepository.findById(orderId)
            .orElseThrow { throw NotFoundException(OrderErrorInfos.ORDER_INFO_NOT_FOUND) }!!
    }
}