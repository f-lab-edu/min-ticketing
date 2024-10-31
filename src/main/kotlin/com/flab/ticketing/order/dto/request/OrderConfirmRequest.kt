package com.flab.ticketing.order.dto.request

import com.flab.ticketing.order.entity.Order

data class OrderConfirmRequest(
    val paymentType: String,
    val orderId: String,
    val paymentKey: String,
    val amount: Int
) {

    fun ofPayments(): Order.Payment {
        return Order.Payment(
            amount,
            paymentType,
            paymentKey
        )
    }


}