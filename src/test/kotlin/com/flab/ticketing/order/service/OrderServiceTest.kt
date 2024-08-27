package com.flab.ticketing.order.service

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.common.OrderTestDataGenerator
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.common.UserTestDataGenerator
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.reader.CartReader
import com.flab.ticketing.order.repository.reader.OrderReader
import com.flab.ticketing.order.repository.writer.CartWriter
import com.flab.ticketing.order.repository.writer.OrderWriter
import com.flab.ticketing.order.service.client.TossPaymentClient
import com.flab.ticketing.user.repository.reader.UserReader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*

class OrderServiceTest : UnitTest() {
    private val userReader: UserReader = mockk()
    private val cartReader: CartReader = mockk()
    private val cartWriter: CartWriter = mockk()
    private val orderReader: OrderReader = mockk()
    private val orderWriter: OrderWriter = mockk()
    private val nanoIdGenerator: NanoIdGenerator = mockk()
    private val tossPaymentClient: TossPaymentClient = mockk()

    private val orderService = OrderService(
        userReader = userReader,
        cartReader = cartReader,
        cartWriter = cartWriter,
        orderReader = orderReader,
        orderWriter = orderWriter,
        nanoIdGenerator = nanoIdGenerator,
        tossPaymentClient = tossPaymentClient
    )

    init {
        "Order 객체를 생성해 DB에 저장하고 주문 정보를 반환할 수 있다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]

            val carts = listOf(
                Cart(
                    "cart001",
                    performance.performancePlace.seats[0],
                    performanceDateTime,
                    user
                ),
                Cart(
                    "cart001",
                    performance.performancePlace.seats[0],
                    performanceDateTime,
                    user
                )
            )
            val orderUid = "order001"

            every { userReader.findByUid(user.uid) } returns user
            every { cartReader.findByUidList(listOf("cart001", "cart002")) } returns carts
            every { nanoIdGenerator.createNanoId() } returns orderUid
            every { orderWriter.save(any()) } returns Unit

            val actual = orderService.saveRequestedOrderInfo(
                AuthenticatedUserDto.of(CustomUserDetailsDto(user.uid, user.email, user.password, user.nickname)),
                OrderInfoRequest("토스 페이", listOf("cart001", "cart002"))
            )

            verify { orderWriter.save(any()) }

            actual.orderId shouldBeEqual orderUid
            actual.amount shouldBeEqual performance.price * 2
            actual.customerName shouldBeEqual user.nickname
            actual.customerEmail shouldBeEqual user.email
            actual.orderName shouldBeEqual performance.name + " 좌석 외 1건"
        }

        "Parameter Cart UID의 갯수와 Repository에서 조회한 Cart의 갯수가 다르면 InvalidValueException을 throw한다." {
            val user = UserTestDataGenerator.createUser()

            every { userReader.findByUid(user.uid) } returns user
            every { cartReader.findByUidList(listOf("cart001", "cart002")) } returns Collections.emptyList()
            every { nanoIdGenerator.createNanoId() } returns "order001"
            every { orderWriter.save(any()) } returns Unit


            val e = shouldThrow<InvalidValueException> {
                orderService.saveRequestedOrderInfo(
                    AuthenticatedUserDto.of(CustomUserDetailsDto(user.uid, user.email, user.password, user.nickname)),
                    OrderInfoRequest("토스 페이", listOf("cart001", "cart002"))
                )
            }

            e.info shouldBe OrderErrorInfos.INVALID_CART_INFO
        }

        "생성된 주문이 존재 할때 Toss 결제 승인 API를 호출하고 Order의 상태를 COMPLETED로 변환할 수 있다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val seats = performance.performancePlace.seats

            val order = OrderTestDataGenerator.createOrder(
                user = user,
                payment = Order.Payment(performance.price * 2, "카드")
            )

            order.addReservation(performanceDateTime, seats[0])

            val orderConfirmRequest = OrderConfirmRequest(
                paymentType = "카드",
                orderId = order.uid,
                paymentKey = "payment001",
                amount = order.payment.totalPrice
            )

            every { orderReader.findByUid(order.uid) } returns order
            every { tossPaymentClient.confirm(orderConfirmRequest) } returns Unit

            orderService.confirmOrder(user.uid, orderConfirmRequest)

            order.status shouldBe Order.OrderStatus.COMPLETED
            verify { tossPaymentClient.confirm(orderConfirmRequest) }
        }
    }


}