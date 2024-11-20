package com.flab.ticketing.testutils.fixture

import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.Reservation
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.user.entity.User
import java.util.concurrent.atomic.AtomicInteger

object OrderFixture {

    private var orderCounter = AtomicInteger()

    fun createOrder(
        user: User,
        payment: Order.Payment = Order.Payment(10000, "KAKAO_PAY", "paymentkey"),
        status: Order.OrderStatus = Order.OrderStatus.COMPLETED
    ): Order {
        return Order(
            "order-00${orderCounter.getAndIncrement()}",
            user,
            payment,
            status
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


    fun clear() {
        orderCounter.set(0)
    }
}