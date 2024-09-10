package com.flab.ticketing.order.repository.dsl

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.order.dto.request.OrderSearchConditions
import com.flab.ticketing.order.entity.Order
interface CustomOrderRepository {

    fun findByUser(userUid: String, cursorInfoDto: CursorInfoDto, searchConditions: OrderSearchConditions ): List<Order>

}