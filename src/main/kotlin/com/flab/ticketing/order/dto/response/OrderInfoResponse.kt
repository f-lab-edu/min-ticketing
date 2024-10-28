package com.flab.ticketing.order.dto.response

import com.flab.ticketing.order.entity.OrderMetaData

data class OrderInfoResponse(
    val orderId: String,
    val amount: Int
) {

    companion object {
        fun of(orderMetaData: OrderMetaData): OrderInfoResponse {
            return OrderInfoResponse(
                orderMetaData.orderId,
                orderMetaData.amount
            )
        }
    }
}