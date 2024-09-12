package com.flab.ticketing.order.service

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.common.OrderTestDataGenerator
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UnitTest
import com.flab.ticketing.common.UserTestDataGenerator
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.exception.BadRequestException
import com.flab.ticketing.common.exception.ForbiddenException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.service.FileService
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.response.OrderSummarySearchResult
import com.flab.ticketing.order.dto.service.TossPayConfirmResponse
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.enums.OrderCancelReasons
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.reader.CartReader
import com.flab.ticketing.order.repository.reader.OrderReader
import com.flab.ticketing.order.repository.writer.CartWriter
import com.flab.ticketing.order.repository.writer.OrderWriter
import com.flab.ticketing.order.service.client.TossPaymentClient
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.user.repository.reader.UserReader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.awt.image.BufferedImage
import java.time.ZonedDateTime
import java.util.*

class OrderServiceTest : UnitTest() {
    private val userReader: UserReader = mockk()
    private val cartReader: CartReader = mockk()
    private val cartWriter: CartWriter = mockk()
    private val orderReader: OrderReader = mockk()
    private val orderWriter: OrderWriter = mockk()
    private val nanoIdGenerator: NanoIdGenerator = mockk()
    private val tossPaymentClient: TossPaymentClient = mockk()
    private val fileService: FileService = mockk()

    private val serviceUrl = "http://test.com"

    private val orderService = OrderService(
        userReader = userReader,
        cartReader = cartReader,
        cartWriter = cartWriter,
        orderReader = orderReader,
        orderWriter = orderWriter,
        nanoIdGenerator = nanoIdGenerator,
        tossPaymentClient = tossPaymentClient,
        fileService = fileService,
        serviceUrl = serviceUrl
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
            every { cartWriter.deleteAll(any()) } returns Unit

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
            every { tossPaymentClient.confirm(orderConfirmRequest) } returns createConfirmResponse()
            val qrImageUrl = "http://test.com/image/1"
            every { fileService.uploadImage(any<BufferedImage>()) } returns qrImageUrl

            orderService.confirmOrder(user.uid, orderConfirmRequest)

            order.status shouldBe Order.OrderStatus.COMPLETED
            verify { tossPaymentClient.confirm(orderConfirmRequest) }
        }

        "Order 객체를 조회하고 이를 OrderSummarySearchResult 객체로 변환해 반환할 수 있다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val seats = performance.performancePlace.seats

            val orders = List(2) {
                OrderTestDataGenerator.createOrder(
                    uid = "order-00${it + 1}",
                    user = user,
                    payment = Order.Payment((it + 1) * 1000, "카드")
                )
            }

            orders.forEachIndexed { index, order -> order.addReservation(performanceDateTime, seats[index]) }

            every { orderReader.findOrderByUser(user.uid, any()) } returns orders

            val actual = orderService.getOrderList(user.uid, CursorInfoDto())
            val expected = listOf(
                OrderSummarySearchResult(
                    "order-001",
                    orders[0].name,
                    performance.image,
                    1000,
                    orders[0].createdAt
                ),
                OrderSummarySearchResult(
                    "order-002",
                    orders[1].name,
                    performance.image,
                    2000,
                    orders[1].createdAt
                ),
            )

            actual shouldContainExactly expected
        }

        "주문을 확정할 때 Reservation의 QR 코드를 생성하여 이미지를 저장하고 DB에 이미지 URL을 저장한다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )

            val order = OrderTestDataGenerator.createOrder(
                user = user,
                payment = Order.Payment(performance.price * 2, "카드")
            )

            order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[0])

            val orderConfirmRequest = OrderConfirmRequest(
                paymentType = "카드",
                orderId = order.uid,
                paymentKey = "payment001",
                amount = order.payment.totalPrice
            )

            every { orderReader.findByUid(order.uid) } returns order
            every { tossPaymentClient.confirm(orderConfirmRequest) } returns createConfirmResponse()

            val qrImageUrl = "http://test.com/image/1"
            every { fileService.uploadImage(any<BufferedImage>()) } returns qrImageUrl

            orderService.confirmOrder(user.uid, orderConfirmRequest)
            order.reservations[0].qrImageUrl!! shouldBeEqual qrImageUrl
        }

        "주문을 OrderSummarySearchResult 객체로 변환할 때 Order 객체에 Reservation 객체가 연관되어 있지 않은 경우 해당 Order 객체는 제외한다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val seats = performance.performancePlace.seats

            val orders = List(3) {
                OrderTestDataGenerator.createOrder(
                    uid = "order-00${it + 1}",
                    user = user,
                    payment = Order.Payment((it + 1) * 1000, "카드")
                )
            }

            orders[0].addReservation(performanceDateTime, seats[0])
            orders[1].addReservation(performanceDateTime, seats[1])

            every { orderReader.findOrderByUser(user.uid, any()) } returns orders

            val actual = orderService.getOrderList(user.uid, CursorInfoDto())

            actual.map { it.uid } shouldContainExactly listOf(orders[0].uid, orders[1].uid)

        }


        "결제 승인시 반환 받는 payment key를 DB에 저장한다." {
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
            val orderConfirmResponse = createConfirmResponse()

            every { orderReader.findByUid(order.uid) } returns order

            every { tossPaymentClient.confirm(orderConfirmRequest) } returns orderConfirmResponse
            val qrImageUrl = "http://test.com/image/1"
            every { fileService.uploadImage(any<BufferedImage>()) } returns qrImageUrl

            orderService.confirmOrder(user.uid, orderConfirmRequest)

            order.status shouldBe Order.OrderStatus.COMPLETED
            order.payment.paymentKey!! shouldBeEqual orderConfirmResponse.paymentKey
            verify { tossPaymentClient.confirm(orderConfirmRequest) }
        }

        "아직 시작되지 않은 공연의 취소 요청이 들어왔을 시 토스 취소 API를 호출하고 공연을 취소한다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(10)
            )
            val order = OrderTestDataGenerator.createOrder(user = user, payment = Order.Payment(1000, "카드", "abc123"))
            order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[0])
            order.status = Order.OrderStatus.COMPLETED

            every { orderReader.findByUid(order.uid) } returns order
            every {
                tossPaymentClient.cancel(
                    order.payment.paymentKey!!,
                    OrderCancelReasons.CUSTOMER_WANTS.reason
                )
            } returns Unit

            orderService.cancelOrder(user.uid, order.uid, OrderCancelReasons.CUSTOMER_WANTS)

            order.status shouldBe Order.OrderStatus.CANCELED
        }

        "공연날짜가 지난 공연의 추소 요청이 들어왔을 시 BadRequestException과 적절한 오류 코드를 반환한다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().minusDays(10)
            )
            val order = OrderTestDataGenerator.createOrder(user = user, payment = Order.Payment(1000, "카드", "abc123"))
            order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[0])

            every { orderReader.findByUid(order.uid) } returns order
            every { tossPaymentClient.cancel(any(), any()) } returns Unit


            val e = shouldThrow<BadRequestException> {
                orderService.cancelOrder(user.uid, order.uid, OrderCancelReasons.CUSTOMER_WANTS)
            }

            e.info shouldBe PerformanceErrorInfos.PERFORMANCE_ALREADY_PASSED

        }

        "주문자와 취소 요청자가 다르다면 ForbiddenException과 적절한 오류 코드를 반환한다." {
            val orderUser = UserTestDataGenerator.createUser()
            val cancelUser = UserTestDataGenerator.createUser(uid = "user-002")
            val performance = PerformanceTestDataGenerator.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(10)
            )
            val order =
                OrderTestDataGenerator.createOrder(user = orderUser, payment = Order.Payment(1000, "카드", "abc123"))
            order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[0])

            every { orderReader.findByUid(order.uid) } returns order
            every { tossPaymentClient.cancel(any(), any()) } returns Unit

            val e = shouldThrow<ForbiddenException> {
                orderService.cancelOrder(cancelUser.uid, order.uid, OrderCancelReasons.CUSTOMER_WANTS)
            }

            e.info shouldBe OrderErrorInfos.INVALID_USER
        }

        "Reservation 중 사용이 완료된 주문이 있다면 ForbiddenException과 적절한 오류 코드를 반환한다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(10)
            )
            val order = OrderTestDataGenerator.createOrder(user = user, payment = Order.Payment(1000, "카드", "abc123"))
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