package com.flab.ticketing.order.dto.response

import java.time.ZonedDateTime

data class OrderDetailSearchResponse(
    val uid: String,
    val orderName: String,
    val orderTime: ZonedDateTime,
    val totalPrice: Int,
    val paymentMethod: String,
    val image: String,
    val reservations: List<ReservationDetailInfo>
) {
    data class ReservationDetailInfo(
        val performanceName: String,
        val performanceTime: ZonedDateTime,
        val qrImage: String,
        val seatName: String
    )
}