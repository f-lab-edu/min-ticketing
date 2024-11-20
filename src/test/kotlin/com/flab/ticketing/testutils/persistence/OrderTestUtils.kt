package com.flab.ticketing.testutils.persistence

import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.order.repository.OrderRepository
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.testutils.generator.OrderTestDataGenerator
import com.flab.ticketing.user.entity.User


/**
 * order 관련 DB 저장을 도와주는 클래스로, Order 저장에 필요한 User와 Performance는 이미 영속화 되어 있다고 가정합니다.
 * @author minseok kim
 */
class OrderTestUtils(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository
) {

    /**
     * cart 객체를 생성하고 저장합니다. <strong>이때 PerformanceDateTime과 PerformancePlaceSeat들이 영속화 되어있는지 확인이 필요합니다.</strong>
     * @author minseok kim
     */
    fun createAndSaveCarts(
        user: User,
        performanceDateTime: PerformanceDateTime,
        seats: List<PerformancePlaceSeat>
    ): List<Cart> {
        val carts = seats.mapIndexed { idx, seat ->
            Cart("cart$idx", seat, performanceDateTime, user)
        }

        cartRepository.saveAll(carts)
        return carts
    }


    /**
     * reservation 정보를 받아 Order 객체를 생성 후 저장합니다.
     * @author minseok kim
     */
    fun createAndSaveOrder(
        user: User,
        performanceDateTime: PerformanceDateTime,
        seats: List<PerformancePlaceSeat>,
        orderPayment: Order.Payment = Order.Payment(10000, "KAKAO_PAY", "paymentkey"),
        orderStatus: Order.OrderStatus = Order.OrderStatus.COMPLETED
    ): Order {
        assert(seats.isNotEmpty())

        val reservations = OrderTestDataGenerator.createReservations(
            performanceDateTime,
            seats,
            OrderTestDataGenerator.createOrder(user = user, payment = orderPayment, status = orderStatus)
        )

        val order = reservations[0].order

        // cascade 정책에 의해 reservation도 함께 저장됩니다.
        orderRepository.save(order)

        return order
    }


    fun saveOrder(order: Order) {
        orderRepository.save(order)
    }

    fun clearContext() {
        // cascade 정책에 의해 reservation도 함께 제거됩니다.
        orderRepository.deleteAll()
        cartRepository.deleteAll()
        OrderTestDataGenerator.clear()
    }

}