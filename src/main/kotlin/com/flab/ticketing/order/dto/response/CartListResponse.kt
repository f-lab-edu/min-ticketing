package com.flab.ticketing.order.dto.response

import com.flab.ticketing.order.entity.Cart
import java.time.ZonedDateTime

data class CartListResponse(
    val data: List<CartInfo>
) {

    companion object {
        fun of(carts: List<Cart>): CartListResponse {
            val cartInfoList = carts.map {
                CartInfo(
                    it.uid,
                    it.performanceDateTime.showTime,
                    it.performanceDateTime.performance.name,
                    it.performanceDateTime.performance.price,
                    it.seat.name
                )
            }.toList()

            return CartListResponse(cartInfoList)
        }
    }

    data class CartInfo(
        val uid: String,
        val performanceDateTime: ZonedDateTime,
        val performanceName: String,
        val performancePrice: Int,
        val seatName: String
    )
}