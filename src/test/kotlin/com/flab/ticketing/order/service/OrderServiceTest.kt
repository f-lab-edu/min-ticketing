package com.flab.ticketing.order.service

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.exception.BadRequestException
import com.flab.ticketing.common.exception.ForbiddenException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.service.FileService
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.request.OrderSearchConditions
import com.flab.ticketing.order.dto.response.OrderDetailInfoResponse
import com.flab.ticketing.order.dto.response.OrderSummarySearchResult
import com.flab.ticketing.order.dto.service.TossPayConfirmResponse
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.OrderMetaData
import com.flab.ticketing.order.enums.OrderCancelReasons
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.reader.CartReader
import com.flab.ticketing.order.repository.reader.OrderReader
import com.flab.ticketing.order.repository.writer.CartWriter
import com.flab.ticketing.order.repository.writer.OrderWriter
import com.flab.ticketing.order.service.client.TossPaymentClient
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.testutils.UnitTest
import com.flab.ticketing.testutils.fixture.OrderFixture
import com.flab.ticketing.testutils.fixture.PerformanceFixture
import com.flab.ticketing.testutils.fixture.UserFixture
import com.flab.ticketing.user.repository.reader.UserReader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.ZonedDateTime
import java.util.*

class OrderServiceTest : UnitTest() {
    private val userReader: UserReader = mockk()
    private val cartReader: CartReader = mockk()
    private val cartWriter: CartWriter = mockk()
    private val orderReader: OrderReader = mockk()
    private val orderWriter: OrderWriter = mockk()
    private val tossPaymentClient: TossPaymentClient = mockk()
    private val fileService: FileService = mockk()

    private val serviceUrl = "http://test.com"

    private val orderService = OrderService(
        userReader = userReader,
        cartReader = cartReader,
        cartWriter = cartWriter,
        orderReader = orderReader,
        orderWriter = orderWriter,
        tossPaymentClient = tossPaymentClient,
        fileService = fileService,
        serviceUrl = serviceUrl
    )

    init {
        "주문 생성 API 호출 시 주문 임시 데이터를 생성하고 Redis에 저장할 수 있다." {
            mockkObject(NanoIdGenerator)
            val user = UserFixture.createUser()
            val performance = PerformanceFixture.createPerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 10)
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


            every { NanoIdGenerator.createNanoId() } returns "tempOrderId"
            every { userReader.findByUid(user.uid) } returns user
            every { cartReader.findByUidList(listOf("cart001", "cart002"), user) } returns carts
            every { orderWriter.save(any<OrderMetaData>()) } just Runs

            val actual = orderService.createOrderMetaData(
                AuthenticatedUserDto.of(CustomUserDetailsDto(user.uid, user.email, user.password, user.nickname)),
                OrderInfoRequest(listOf("cart001", "cart002"))
            )


            verify(exactly = 1) { orderWriter.save(any<OrderMetaData>()) }
            actual.amount shouldBeEqual performance.price * 2
            actual.orderId shouldBeEqual "tempOrderId"
        }

        "Parameter Cart UID의 갯수와 Repository에서 조회한 Cart의 갯수가 다르면 InvalidValueException을 throw한다." {
            mockkObject(NanoIdGenerator)

            val user = UserFixture.createUser()

            every { userReader.findByUid(user.uid) } returns user
            every { cartReader.findByUidList(listOf("cart001", "cart002"), user) } returns Collections.emptyList()
            every { NanoIdGenerator.createNanoId() } returns "order001"

            val e = shouldThrow<InvalidValueException> {
                orderService.createOrderMetaData(
                    AuthenticatedUserDto.of(CustomUserDetailsDto(user.uid, user.email, user.password, user.nickname)),
                    OrderInfoRequest(listOf("cart001", "cart002"))
                )
            }

            e.info shouldBe OrderErrorInfos.INVALID_CART_INFO
        }


        "생성된 주문(OrderMetaData)이 존재 할때 Toss 결제 승인 API를 호출하고 Order 정보를 DB에 저장한다." {
            val user = UserFixture.createUser()
            val performance = PerformanceFixture.createPerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 10)
            )
            val carts =
                listOf(
                    Cart("cart001", performance.performancePlace.seats[0], performance.performanceDateTime[0], user),
                    Cart("cart002", performance.performancePlace.seats[1], performance.performanceDateTime[0], user)
                )

            val qrImageUrl = "http://test.com/image/1"

            val orderMetaData = OrderMetaData(
                orderId = "order1",
                amount = performance.price * 2,
                cartUidList = listOf("cart1", "cart2"),
                userUid = user.uid
            )


            val orderConfirmRequest = OrderConfirmRequest(
                paymentType = "카드",
                orderId = orderMetaData.orderId,
                paymentKey = "payment001",
                amount = performance.price * 2
            )

            every { userReader.findByUid(user.uid) } returns user
            every { cartReader.findByUidList(any(), user) } returns carts
            every { orderReader.findMetaData(orderMetaData.orderId) } returns orderMetaData
            every { tossPaymentClient.confirm(orderConfirmRequest) } returns createConfirmResponse()
            every { orderWriter.save(any<Order>()) } just Runs
            every { orderWriter.deleteMetaData(orderMetaData) } just Runs
            every { fileService.uploadImage(any()) } returns qrImageUrl
            every { cartWriter.deleteAll(carts) } just Runs

            orderService.confirmOrder(user.uid, orderConfirmRequest)

            verify(exactly = 1) { tossPaymentClient.confirm(orderConfirmRequest) }
            verify(exactly = 1) { orderWriter.save(any<Order>()) }
            verify(exactly = 1) { orderWriter.deleteMetaData(orderMetaData) }
            verify(exactly = carts.size) { fileService.uploadImage(any()) }
            verify(exactly = 1) { cartWriter.deleteAll(carts) }
        }

        "Order 객체를 조회하고 이를 OrderSummarySearchResult 객체로 변환해 반환할 수 있다." {
            val user = UserFixture.createUser()
            val performance = PerformanceFixture.createPerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val seats = performance.performancePlace.seats

            val orders = List(2) {
                OrderFixture.createOrder(
                    user = user,
                    payment = Order.Payment(1000, "카드", "paymentkey")
                )
            }

            orders.forEachIndexed { index, order -> order.addReservation(performanceDateTime, seats[index]) }

            every { orderReader.findOrderByUser(user.uid, any(), any()) } returns orders

            val actual = orderService.getOrderList(user.uid, OrderSearchConditions(), CursorInfoDto())
            val expected = orders.map {
                OrderSummarySearchResult(
                    it.uid,
                    it.name,
                    performance.image,
                    1000,
                    it.createdAt
                )

            }

            actual shouldContainExactly expected
        }

        "주문을 OrderSummarySearchResult 객체로 변환할 때 Order 객체에 Reservation 객체가 연관되어 있지 않은 경우 해당 Order 객체는 제외한다." {
            val user = UserFixture.createUser()
            val performance = PerformanceFixture.createPerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val seats = performance.performancePlace.seats

            val orders = List(3) {
                OrderFixture.createOrder(
                    user = user,
                    payment = Order.Payment((it + 1) * 1000, "카드", "paymentkey")
                )
            }

            orders[0].addReservation(performanceDateTime, seats[0])
            orders[1].addReservation(performanceDateTime, seats[1])

            every { orderReader.findOrderByUser(user.uid, any(), any()) } returns orders

            val actual = orderService.getOrderList(user.uid, OrderSearchConditions(), CursorInfoDto())

            actual.map { it.uid } shouldContainExactly listOf(orders[0].uid, orders[1].uid)

        }


        "아직 시작되지 않은 공연의 취소 요청이 들어왔을 시 토스 취소 API를 호출하고 공연을 취소한다." {
            val user = UserFixture.createUser()
            val performance = PerformanceFixture.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(10)
            )
            val order = OrderFixture.createOrder(user = user, payment = Order.Payment(1000, "카드", "abc123"))
            order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[0])
            order.status = Order.OrderStatus.COMPLETED

            every { orderReader.findByUid(order.uid) } returns order
            every {
                tossPaymentClient.cancel(
                    order.payment.paymentKey,
                    OrderCancelReasons.CUSTOMER_WANTS.reason
                )
            } returns Unit

            orderService.cancelOrder(user.uid, order.uid, OrderCancelReasons.CUSTOMER_WANTS)

            order.status shouldBe Order.OrderStatus.CANCELED
        }

        "공연날짜가 지난 공연의 추소 요청이 들어왔을 시 BadRequestException과 적절한 오류 코드를 반환한다." {
            val user = UserFixture.createUser()
            val performance = PerformanceFixture.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().minusDays(10)
            )
            val order = OrderFixture.createOrder(user = user, payment = Order.Payment(1000, "카드", "abc123"))
            order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[0])

            every { orderReader.findByUid(order.uid) } returns order
            every { tossPaymentClient.cancel(any(), any()) } returns Unit


            val e = shouldThrow<BadRequestException> {
                orderService.cancelOrder(user.uid, order.uid, OrderCancelReasons.CUSTOMER_WANTS)
            }

            e.info shouldBe PerformanceErrorInfos.PERFORMANCE_ALREADY_PASSED

        }

        "주문자와 취소 요청자가 다르다면 ForbiddenException과 적절한 오류 코드를 반환한다." {
            val orderUser = UserFixture.createUser()
            val cancelUser = UserFixture.createUser(uid = "user-002")
            val performance = PerformanceFixture.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(10)
            )
            val order =
                OrderFixture.createOrder(user = orderUser, payment = Order.Payment(1000, "카드", "abc123"))
            order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[0])

            every { orderReader.findByUid(order.uid) } returns order
            every { tossPaymentClient.cancel(any(), any()) } returns Unit

            val e = shouldThrow<ForbiddenException> {
                orderService.cancelOrder(cancelUser.uid, order.uid, OrderCancelReasons.CUSTOMER_WANTS)
            }

            e.info shouldBe OrderErrorInfos.INVALID_USER
        }

        "주문 취소시 Reservation 중 사용이 완료된 주문이 있다면 ForbiddenException과 적절한 오류 코드를 반환한다." {
            val user = UserFixture.createUser()
            val performance = PerformanceFixture.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(10)
            )
            val order = OrderFixture.createOrder(user = user, payment = Order.Payment(1000, "카드", "abc123"))
            order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[0])
            order.status = Order.OrderStatus.COMPLETED
            order.reservations[0].isUsed = true

            every { orderReader.findByUid(order.uid) } returns order
            every { tossPaymentClient.cancel(any(), any()) } returns Unit

            val e = shouldThrow<ForbiddenException> {
                orderService.cancelOrder(user.uid, order.uid, OrderCancelReasons.CUSTOMER_WANTS)
            }

            e.info shouldBe OrderErrorInfos.RESERVATION_ALREADY_USED

        }

        "사용자는 자신의 주문 상세 정보를 조회할 수 있다." {
            // given
            val user = UserFixture.createUser()
            val performance = PerformanceFixture.createPerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 1),
                price = 1000
            )

            val order = OrderFixture.createOrder(
                user = user,
                payment = Order.Payment(1000, "카드", "paymentkey"),
            )
            OrderFixture.createReservations(
                performanceDateTime = performance.performanceDateTime[0],
                seats = performance.performancePlace.seats.subList(0, 1),
                order
            )


            every { orderReader.findByUid(order.uid) } returns order

            // when
            val actual = orderService.getOrderDetail(order.uid, user.uid)

            // then
            actual.orderUid shouldBeEqual order.uid
            actual.orderedAt shouldBeEqual order.createdAt
            actual.orderStatus shouldBeEqual order.status
            actual.totalPrice shouldBe order.payment.totalPrice
            actual.orderName shouldBeEqual order.name
            actual.reservations shouldBe listOf(
                OrderDetailInfoResponse.ReservationDetailInfo(
                    performance.name,
                    performance.price,
                    order.reservations[0].qrImageUrl!!,
                    false
                )
            )

        }

        "사용자가 자신이 아닌 다른 사람의 주문 상세 조회 정보 조회 시도시 ForbiddenException을 throw한다." {
            // given
            val user = UserFixture.createUser()
            val user2 = UserFixture.createUser(uid = "useruid002")
            val performance = PerformanceFixture.createPerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 1),
                price = 1000
            )

            val order = OrderFixture.createOrder(
                user = user,
                payment = Order.Payment(1000, "카드", "paymentkey"),
            )
            OrderFixture.createReservations(
                performanceDateTime = performance.performanceDateTime[0],
                seats = performance.performancePlace.seats.subList(0, 1),
                order
            )


            every { orderReader.findByUid(order.uid) } returns order
            // when
            shouldThrow<ForbiddenException> {
                orderService.getOrderDetail(order.uid, user2.uid)
            }.info shouldBe OrderErrorInfos.INVALID_USER

        }
    }


    fun createConfirmResponse(): TossPayConfirmResponse {
        return TossPayConfirmResponse(
            mId = "tosspayments",
            lastTransactionKey = "9C62B18EEF0DE3EB7F4422EB6D14BC6E",
            paymentKey = "5EnNZRJGvaBX7zk2yd8ydw26XvwXkLrx9POLqKQjmAw4b0e1",
            orderId = "a4CWyWY5m89PNh7xJwhk1",
            orderName = "토스 티셔츠 외 2건",
            taxExemptionAmount = 0,
            status = "DONE",
            requestedAt = "2024-02-13T12:17:57+09:00",
            approvedAt = "2024-02-13T12:18:14+09:00",
            useEscrow = false,
            cultureExpense = false,
            card = TossPayConfirmResponse.Card(
                issuerCode = "71",
                acquirerCode = "71",
                number = "12345678****000*",
                installmentPlanMonths = 0,
                isInterestFree = false,
                interestPayer = null,
                approveNo = "00000000",
                useCardPoint = false,
                cardType = "신용",
                ownerType = "개인",
                acquireStatus = "READY",
                receiptUrl = "https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20240213121757MvuS8&ref=PX",
                amount = 1000
            ),
            virtualAccount = null,
            transfer = null,
            mobilePhone = null,
            giftCertificate = null,
            cashReceipt = null,
            cashReceipts = null,
            discount = null,
            cancels = null,
            secret = null,
            type = "NORMAL",
            easyPay = TossPayConfirmResponse.EasyPay(
                provider = "토스페이",
                amount = 0,
                discountAmount = 0
            ),
            easyPayAmount = 0,
            easyPayDiscountAmount = 0,
            country = "KR",
            failure = null,
            isPartialCancelable = true,
            receipt = TossPayConfirmResponse.Receipt(
                url = "https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20240213121757MvuS8&ref=PX"
            ),
            checkout = TossPayConfirmResponse.Checkout(
                url = "https://api.tosspayments.com/v1/payments/5EnNZRJGvaBX7zk2yd8ydw26XvwXkLrx9POLqKQjmAw4b0e1/checkout"
            ),
            currency = "KRW",
            totalAmount = 1000,
            balanceAmount = 1000,
            suppliedAmount = 909,
            vat = 91,
            taxFreeAmount = 0,
            method = "카드",
            version = "2022-11-16"
        )
    }

}