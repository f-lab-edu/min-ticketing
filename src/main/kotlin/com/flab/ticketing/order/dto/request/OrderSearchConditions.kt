package com.flab.ticketing.order.dto.request

import com.flab.ticketing.order.entity.Order

data class OrderSearchConditions(
    val status: Order.OrderStatus? = null
)