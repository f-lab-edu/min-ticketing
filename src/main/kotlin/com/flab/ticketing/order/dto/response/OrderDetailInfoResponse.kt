package com.flab.ticketing.order.dto.response

import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.Reservation
import java.time.ZonedDateTime

data class OrderDetailInfoResponse(
    val orderUid: String,
    val totalPrice: Int,
    val orderStatus: Order.OrderStatus,
    val orderedAt: ZonedDateTime,
    val paymentMethod: String,
    val reservations: List<ReservationDetailInfo>
) {


    companion object {

        /**
         *  OrderDetailInfoResponse을 생성합니다. reservations에서 N + 1문제가 발생할 수 있으니 주의가 필요합니다.
         * @author minseok kim
         */
        fun of(order: Order): OrderDetailInfoResponse {
            return OrderDetailInfoResponse(
                orderUid = order.uid,
                totalPrice = order.payment.totalPrice,
                orderStatus = order.status,
                orderedAt = order.createdAt,
                paymentMethod = order.payment.paymentMethod,
                reservations = order.reservations.map { ReservationDetailInfo.of(it) }
            )

        }

    }

    data class ReservationDetailInfo(
        val performanceName: String,
        val performancePrice: Int,
        val qrImage: String,
        val isUsed: Boolean
    ) {
        companion object {
            fun of(reservation: Reservation): ReservationDetailInfo {
                return ReservationDetailInfo(
                    performanceName = reservation.performanceDateTime.performance.name,
                    performancePrice = reservation.performanceDateTime.performance.price,
                    qrImage = reservation.qrImageUrl!!,
                    isUsed = reservation.isUsed
                )
            }
        }

    }


}