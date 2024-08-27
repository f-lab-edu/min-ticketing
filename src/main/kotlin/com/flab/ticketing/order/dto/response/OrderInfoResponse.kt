package com.flab.ticketing.order.dto.response

data class OrderInfoResponse(
    val orderName: String,
    val orderId: String,
    val customerEmail: String,
    val customerName: String,
    val amount: Int
)