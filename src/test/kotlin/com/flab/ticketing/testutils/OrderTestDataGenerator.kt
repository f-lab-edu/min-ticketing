package com.flab.ticketing.testutils

import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.Reservation
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.user.entity.User

object OrderTestDataGenerator {

    fun createOrder(
        uid: String = "order-001",
        user: User,
        payment: Order.Payment = Order.Payment(10000, "KAKAO_PAY", "paymentkey")
    ): Order {
        return Order(
            uid,
            user,
            payment
        )
    }

    fun createReservations(
        performanceDateTime: PerformanceDateTime,
        seats: List<PerformancePlaceSeat>,
        order: Order
    ): List<Reservation> {
        val result = mutableListOf<Reservation>()

        for (seat in seats) {
            val reservation = Reservation(
                performanceDateTime,
                seat,
                order
            )
            order.addReservation(reservation)
            result.add(reservation)
        }

        return result
    }

}