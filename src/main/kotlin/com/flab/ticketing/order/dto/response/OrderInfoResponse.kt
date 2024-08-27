package com.flab.ticketing.order.dto.response

import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.user.entity.User

data class OrderInfoResponse(
    val orderName: String,
    val orderId: String,
    val customerEmail: String,
    val customerName: String,
    val amount: Int
) {

    companion object {
        fun of(user: User, order: Order): OrderInfoResponse {
            return OrderInfoResponse(
                order.name,
                order.uid,
                user.email,
                user.nickname,
                order.payment.totalPrice
            )
        }
    }
}