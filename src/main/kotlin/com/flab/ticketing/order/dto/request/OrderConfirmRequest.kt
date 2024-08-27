package com.flab.ticketing.order.dto.request

data class OrderConfirmRequest(
    val paymentType: String,
    val orderId: String,
    val paymentKey: String,
    val amount: Int
)