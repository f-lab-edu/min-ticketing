package com.flab.ticketing.order.service

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.common.UserTestDataGenerator
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.repository.reader.CartReader
import com.flab.ticketing.order.repository.writer.CartWriter
import com.flab.ticketing.order.repository.writer.OrderWriter
import com.flab.ticketing.user.repository.reader.UserReader
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class OrderServiceTest : UnitTest() {
    private val userReader: UserReader = mockk()
    private val cartReader: CartReader = mockk()
    private val cartWriter: CartWriter = mockk()
    private val orderWriter: OrderWriter = mockk()
    private val nanoIdGenerator: NanoIdGenerator = mockk()


    private val orderService = OrderService(
        userReader = userReader,
        cartReader = cartReader,
        cartWriter = cartWriter,
        orderWriter = orderWriter,
        nanoIdGenerator = nanoIdGenerator
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
            every { cartWriter.deleteAll(carts) } returns Unit

            val actual = orderService.saveRequestedOrderInfo(
                AuthenticatedUserDto.of(CustomUserDetailsDto(user.uid, user.email, user.password, user.nickname)),
                OrderInfoRequest("토스 페이", listOf("cart001", "cart002"))
            )

            verify { orderWriter.save(any()) }
            verify { cartWriter.deleteAll(carts) }

            actual.orderId shouldBeEqual orderUid
            actual.amount shouldBeEqual performance.price * 2
            actual.customerName shouldBeEqual user.nickname
            actual.customerEmail shouldBeEqual user.email
            actual.orderName shouldBeEqual performance.name + " 좌석 외 1건"
        }
    }


}