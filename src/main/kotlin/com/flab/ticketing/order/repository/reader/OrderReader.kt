package com.flab.ticketing.order.repository.reader

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.order.dto.response.OrderSummarySearchResult
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.OrderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class OrderReader(
    private val orderRepository: OrderRepository
) {

    fun findByUid(uid: String): Order {
        return orderRepository.findByUid(uid) ?: throw NotFoundException(OrderErrorInfos.ORDER_INFO_NOT_FOUND)
    }

    fun findOrderByUser(userUid: String, cursorInfo: CursorInfoDto): List<Order> {
        return cursorInfo.cursor?.let {
            orderRepository.findByUser(userUid, cursorInfo.cursor, PageRequest.of(0, cursorInfo.limit))
        } ?: orderRepository.findByUser(userUid, PageRequest.of(0, cursorInfo.limit))
    }
}