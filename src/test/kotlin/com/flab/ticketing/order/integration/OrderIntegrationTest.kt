package com.flab.ticketing.order.integration

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.OrderTestDataGenerator
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UserTestDataGenerator
import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.response.OrderDetailSearchResponse
import com.flab.ticketing.order.dto.response.OrderInfoResponse
import com.flab.ticketing.order.dto.response.OrderSummarySearchResult
import com.flab.ticketing.order.dto.service.TossPayErrorResponse
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.enums.TossPayErrorCode
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.order.repository.OrderRepository
import com.flab.ticketing.order.service.client.TossPaymentClient.Companion.TOSS_EXCEPTION_PREFIX
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.UserRepository
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAll
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit


@Transactional
class OrderIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var cartRepository: CartRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var regionRepository: RegionRepository

    @Autowired
    private lateinit var placeRepository: PerformancePlaceRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider


    init {

        given("사용자의 장바구니 정보가 존재할 때") {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val performancePlace = performance.performancePlace


            val carts = createCarts(user, performanceDateTime, performancePlace.seats.subList(0, 5))

            savePerformance(listOf(performance))
            userRepository.save(user)
            cartRepository.saveAll(carts)


            `when`("장바구니를 선택하여 주문 정보를 생성할 시") {
                val uri = "/api/orders/toss/info"
                val orderCartUidList = carts.subList(0, 3).map { it.uid }
                val orderRequest = OrderInfoRequest("토스 뱅크", orderCartUidList)
                val jwt = createJwt(user)


                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest))
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("주문 정보를 생성하여 반환하며, DB에 임시 주문 정보를 저장하며 Cart 정보를 DB에서 제거한다.") {
                    mvcResult.response.status shouldBe HttpStatus.CREATED.value()
                    val actual =
                        objectMapper.readValue(mvcResult.response.contentAsString, OrderInfoResponse::class.java)

                    val savedOrder = orderRepository.findByUser(user.uid)[0]
                    val expectedReservedSeatAndDateTime = listOf(
                        Pair(performancePlace.seats[0], performanceDateTime),
                        Pair(performancePlace.seats[1], performanceDateTime),
                        Pair(performancePlace.seats[2], performanceDateTime)
                    )


                    actual.customerEmail shouldBeEqual user.email
                    actual.customerName shouldBeEqual user.nickname
                    actual.amount shouldBe (performance.price * orderCartUidList.size)
                    actual.orderId shouldBeEqual savedOrder.uid

                    savedOrder.payment.paymentMethod shouldBeEqual orderRequest.payType
                    savedOrder.payment.totalPrice shouldBe actual.amount
                    savedOrder.user shouldBeEqual user
                    savedOrder.reservations.map {
                        Pair(
                            it.seat,
                            it.performanceDateTime
                        )
                    } shouldContainAll expectedReservedSeatAndDateTime

                    cartRepository.findByUserUid(user.uid).map { it.uid } shouldNotContainAll orderCartUidList
                }
            }

        }

        given("사용자의 장바구니 정보가 존재할 때 - 잘못된 Cart UID 포함") {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val performancePlace = performance.performancePlace


            val carts = createCarts(user, performanceDateTime, performancePlace.seats.subList(0, 5))

            savePerformance(listOf(performance))
            userRepository.save(user)
            cartRepository.saveAll(carts)

            `when`("존재하지 않는 Cart UID로 주문 정보 생성 API 호출 시") {
                val uri = "/api/orders/toss/info"
                val orderCartUidList = carts.subList(0, 3).map { it.uid }.toMutableList()
                orderCartUidList.add("invalidCart001")

                val orderRequest = OrderInfoRequest("토스 뱅크", orderCartUidList)
                val jwt = createJwt(user)


                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest))
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 Bad Request를 출력한다.") {
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, OrderErrorInfos.INVALID_CART_INFO)
                }
            }
        }

        given("주문 생성이 완료되었을 때") {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val seats = performance.performancePlace.seats

            userRepository.save(user)
            savePerformance(listOf(performance))

            val order = OrderTestDataGenerator.createOrder(
                user = user,
                payment = Order.Payment(performance.price * 2, "카드")
            )

            order.addReservation(performanceDateTime, seats[0])
            order.addReservation(performanceDateTime, seats[1])

            orderRepository.save(order)

            val orderConfirmRequest = OrderConfirmRequest(
                paymentType = "카드",
                orderId = order.uid,
                paymentKey = "payment001",
                amount = order.payment.totalPrice
            )

            setUpTossPaymentConfirmResponse(orderConfirmRequest)
            `when`("결제 승인을 요청할 시") {
                val uri = "/api/orders/toss/confirm"
                val jwt = createJwt(user)

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderConfirmRequest))
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("토스 결제 승인 API를 호출하고, Order의 상태를 COMPLETED로 바꾼다.") {
                    mvcResult.response.status shouldBe HttpStatus.OK.value()
                    orderRepository.findByUid(order.uid)!!.status shouldBe Order.OrderStatus.COMPLETED
                }
            }
        }

        given("주문 생성이 완료 되었을 때 - 잘못된 유저 요청") {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val seats = performance.performancePlace.seats

            userRepository.save(user)
            savePerformance(listOf(performance))

            val order = OrderTestDataGenerator.createOrder(
                user = user,
                payment = Order.Payment(performance.price * 2, "카드")
            )

            order.addReservation(performanceDateTime, seats[0])
            order.addReservation(performanceDateTime, seats[1])

            orderRepository.save(order)

            val orderConfirmRequest = OrderConfirmRequest(
                paymentType = "카드",
                orderId = order.uid,
                paymentKey = "payment001",
                amount = order.payment.totalPrice
            )
            `when`("다른 유저로 Order 주문 확정 API 호출 시") {
                val user2 = UserTestDataGenerator.createUser(
                    uid = "user002",
                    email = "email2@email.com"
                )
                userRepository.save(user)
                val jwt = createJwt(user2)

                val uri = "/api/orders/toss/confirm"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderConfirmRequest))
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 BAD Request와 적절한 오류 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, OrderErrorInfos.INVALID_USER)
                }
            }
        }

        given("주문 생성이 완료 되었을 때 - 토스 페이 API 정상 응답이 아닌 경우") {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val seats = performance.performancePlace.seats

            userRepository.save(user)
            savePerformance(listOf(performance))

            val order = OrderTestDataGenerator.createOrder(
                user = user,
                payment = Order.Payment(performance.price * 2, "카드")
            )

            order.addReservation(performanceDateTime, seats[0])
            order.addReservation(performanceDateTime, seats[1])

            orderRepository.save(order)

            val orderConfirmRequest = OrderConfirmRequest(
                paymentType = "카드",
                orderId = order.uid,
                paymentKey = "payment001",
                amount = order.payment.totalPrice
            )
            val tossPayResponse = setUpTossPaymentFailResponse()

            `when`("결제 승인 API를 호출 시") {
                val uri = "/api/orders/toss/confirm"
                val jwt = createJwt(user)

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderConfirmRequest))
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("토스 페이 API의 응답 정보를 반환하고, 주문을 PENDING 상태로 변경한다.") {
                    checkError(
                        mvcResult,
                        HttpStatus.NOT_FOUND,
                        CommonErrorInfos.EXTERNAL_API_ERROR.code,
                        TOSS_EXCEPTION_PREFIX + tossPayResponse.message
                    )
                    orderRepository.findByUid(order.uid)!!.status shouldBe Order.OrderStatus.PENDING
                }
            }

        }

        given("사용자의 주문 정보가 존재할 때") {
            val user = UserTestDataGenerator.createUser()
            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 2,
                seatPerPlace = 5
            )
            val order1 = OrderTestDataGenerator.createOrder(
                user = user
            )
            val order2 = OrderTestDataGenerator.createOrder(
                uid = "order-002",
                user = user
            )
            val order3 = OrderTestDataGenerator.createOrder(
                uid = "order-003",
                user = user
            )
            order1.addReservation(performances[0].performanceDateTime[0], performances[0].performancePlace.seats[0])
            order2.addReservation(performances[1].performanceDateTime[0], performances[1].performancePlace.seats[0])
            order3.addReservation(performances[1].performanceDateTime[0], performances[1].performancePlace.seats[1])


            userRepository.save(user)
            savePerformance(performances)
            orderRepository.save(order1)
            orderRepository.save(order2)
            orderRepository.save(order3)
            `when`("주문 정보 리스트 조회시") {
                val uri = "/api/orders"
                val jwt = createJwt(user)
                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("사용자가 주문한 주문 정보 리스트를 주문 생성 순으로 정렬해 반환한다.") {
                    val actual = objectMapper.readValue<CursoredResponse<OrderSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            OrderSummarySearchResult::class.java
                        )
                    )
                    val expected = listOf(
                        OrderSummarySearchResult(
                            order3.uid,
                            order3.name,
                            order3.reservations[0].performanceDateTime.performance.image,
                            order3.payment.totalPrice,
                            order3.createdAt.withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)
                        ),
                        OrderSummarySearchResult(
                            order2.uid,
                            order2.name,
                            order2.reservations[0].performanceDateTime.performance.image,
                            order2.payment.totalPrice,
                            order2.createdAt.withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)
                        )
                    )


                    actual.cursor shouldBe order1.uid
                    actual.data.forEachIndexed { index, actualResult ->
                        val expectedResult = expected[index]
                        actualResult.uid shouldBe expectedResult.uid
                        actualResult.name shouldBe expectedResult.name
                        actualResult.performanceImage shouldBe expectedResult.performanceImage
                        actualResult.totalPrice shouldBe expectedResult.totalPrice
                        actualResult.orderedTime.truncatedTo(ChronoUnit.SECONDS) shouldBe expectedResult.orderedTime
                    }

                }
            }

        }


        given("사용자의 주문 정보가 존재할 때 - 주문 상세 조회") {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val seats = performance.performancePlace.seats
            val performanceDateTime = performance.performanceDateTime[0]

            val order = OrderTestDataGenerator.createOrder(
                user = user
            )

            for (i in 0..2) {
                order.addReservation(performanceDateTime, seats[i])
            }

            order.reservations.forEach {
                it.qrImageUrl = "http://qrImage.com/images/${it.id}"
            }

            userRepository.save(user)
            savePerformance(listOf(performance))
            orderRepository.save(order)

            `when`("주문 정보를 상세 조회할 시") {
                val uri = "/api/orders/${order.uid}"
                val jwt = createJwt(user)

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("해당 주문 정보의 상세 정보를 반환한다.") {
                    val actual = objectMapper.readValue(
                        mvcResult.response.contentAsString,
                        OrderDetailSearchResponse::class.java
                    )

                    val expectedReservationInfo = listOf(
                        OrderDetailSearchResponse.ReservationDetailInfo(
                            performanceName = performance.name,
                            performanceTime = performanceDateTime.showTime.withZoneSameInstant(
                                ZoneOffset.UTC
                            ).truncatedTo(
                                ChronoUnit.SECONDS
                            ),
                            qrImage = order.reservations[0].qrImageUrl!!,
                            seatName = order.reservations[0].seat.name
                        ),
                        OrderDetailSearchResponse.ReservationDetailInfo(
                            performanceName = performance.name,
                            performanceTime = performanceDateTime.showTime.withZoneSameInstant(
                                ZoneOffset.UTC
                            ).truncatedTo(
                                ChronoUnit.SECONDS
                            ),
                            qrImage = order.reservations[1].qrImageUrl!!,
                            seatName = order.reservations[1].seat.name
                        ), OrderDetailSearchResponse.ReservationDetailInfo(
                            performanceName = performance.name,
                            performanceTime = performanceDateTime.showTime.withZoneSameInstant(
                                ZoneOffset.UTC
                            ).truncatedTo(
                                ChronoUnit.SECONDS
                            ),
                            qrImage = order.reservations[2].qrImageUrl!!,
                            seatName = order.reservations[2].seat.name
                        )
                    )

                    actual.uid shouldBeEqual order.uid
                    actual.orderName shouldBeEqual order.name
                    actual.orderTime.truncatedTo(ChronoUnit.SECONDS) shouldBeEqual order.createdAt.withZoneSameInstant(
                        ZoneOffset.UTC
                    ).truncatedTo(
                        ChronoUnit.SECONDS
                    )

                    actual.totalPrice shouldBe order.payment.totalPrice
                    actual.paymentMethod shouldBeEqual order.payment.paymentMethod
                    actual.image shouldBeEqual order.reservations[0].performanceDateTime.performance.image
                    actual.reservations shouldContainAll expectedReservationInfo
                }
            }

        }

        given("사용자의 주문 정보가 존재하지 않을 때 - 주문 상세 조회 불가") {
            val user = UserTestDataGenerator.createUser()

            userRepository.save(user)
            `when`("주문 정보를 상세 조회할 시") {
                val uri = "/api/orders/unknown001"
                val jwt = createJwt(user)

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("404 오류와 적절한 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.NOT_FOUND, OrderErrorInfos.ORDER_INFO_NOT_FOUND)
                }
            }
        }

        given("사용자의 주문 정보가 존재할 때 - 주문 상세 조회, 다른 사용자") {
            val user = UserTestDataGenerator.createUser()
            val user2 = UserTestDataGenerator.createUser(uid = "user-002", email = "email2@email.com")
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val seats = performance.performancePlace.seats
            val performanceDateTime = performance.performanceDateTime[0]

            val order = OrderTestDataGenerator.createOrder(
                user = user
            )

            order.addReservation(performanceDateTime, seats[0])

            order.reservations.forEach {
                it.qrImageUrl = "http://qrImage.com/images/${it.id}"
            }

            userRepository.saveAll(listOf(user, user2))
            savePerformance(listOf(performance))
            orderRepository.save(order)

            `when`("주문 정보를 상세 조회할 시") {
                val uri = "/api/orders/${order.uid}"
                val jwt = createJwt(user2)

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("403 오류와 적절한 오류 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.FORBIDDEN, CommonErrorInfos.UNREACHABLE_RESOURCE)

                }

            }
        }

    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)

        PerformanceTestDataGenerator.reset()
        withContext(Dispatchers.IO) {
            cartRepository.deleteAll()
            orderRepository.deleteAll()
            userRepository.deleteAll()
            performanceRepository.deleteAll()
            placeRepository.deleteAll()
            regionRepository.deleteAll()
        }
    }

    private fun setUpTossPaymentConfirmResponse(orderConfirmRequest: OrderConfirmRequest) {
        mockServerUtils.addMockResponse(
            HttpStatus.OK,
            "{\n" +
                    "  \"mId\": \"tosspayments\",\n" +
                    "  \"lastTransactionKey\": \"9C62B18EEF0DE3EB7F4422EB6D14BC6E\",\n" +
                    "  \"paymentKey\": \"${orderConfirmRequest.paymentKey}\",\n" +
                    "  \"orderId\": \"${orderConfirmRequest.orderId}\",\n" +
                    "  \"orderName\": \"토스 티셔츠 외 2건\",\n" +
                    "  \"taxExemptionAmount\": 0,\n" +
                    "  \"status\": \"DONE\",\n" +
                    "  \"requestedAt\": \"2024-02-13T12:17:57+09:00\",\n" +
                    "  \"approvedAt\": \"2024-02-13T12:18:14+09:00\",\n" +
                    "  \"useEscrow\": false,\n" +
                    "  \"cultureExpense\": false,\n" +
                    "  \"card\": {\n" +
                    "    \"issuerCode\": \"71\",\n" +
                    "    \"acquirerCode\": \"71\",\n" +
                    "    \"number\": \"12345678****000*\",\n" +
                    "    \"installmentPlanMonths\": 0,\n" +
                    "    \"isInterestFree\": false,\n" +
                    "    \"interestPayer\": null,\n" +
                    "    \"approveNo\": \"00000000\",\n" +
                    "    \"useCardPoint\": false,\n" +
                    "    \"cardType\": \"신용\",\n" +
                    "    \"ownerType\": \"개인\",\n" +
                    "    \"acquireStatus\": \"READY\",\n" +
                    "    \"receiptUrl\": \"https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20240213121757MvuS8&ref=PX\",\n" +
                    "    \"amount\": 1000\n" +
                    "  },\n" +
                    "  \"virtualAccount\": null,\n" +
                    "  \"transfer\": null,\n" +
                    "  \"mobilePhone\": null,\n" +
                    "  \"giftCertificate\": null,\n" +
                    "  \"cashReceipt\": null,\n" +
                    "  \"cashReceipts\": null,\n" +
                    "  \"discount\": null,\n" +
                    "  \"cancels\": null,\n" +
                    "  \"secret\": null,\n" +
                    "  \"type\": \"NORMAL\",\n" +
                    "  \"easyPay\": {\n" +
                    "    \"provider\": \"토스페이\",\n" +
                    "    \"amount\": 0,\n" +
                    "    \"discountAmount\": 0\n" +
                    "  },\n" +
                    "  \"easyPayAmount\": 0,\n" +
                    "  \"easyPayDiscountAmount\": 0,\n" +
                    "  \"country\": \"KR\",\n" +
                    "  \"failure\": null,\n" +
                    "  \"isPartialCancelable\": true,\n" +
                    "  \"receipt\": {\n" +
                    "    \"url\": \"https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20240213121757MvuS8&ref=PX\"\n" +
                    "  },\n" +
                    "  \"checkout\": {\n" +
                    "    \"url\": \"https://api.tosspayments.com/v1/payments/5EnNZRJGvaBX7zk2yd8ydw26XvwXkLrx9POLqKQjmAw4b0e1/checkout\"\n" +
                    "  },\n" +
                    "  \"currency\": \"KRW\",\n" +
                    "  \"totalAmount\": ${orderConfirmRequest.amount},\n" +
                    "  \"balanceAmount\": 1000,\n" +
                    "  \"suppliedAmount\": 909,\n" +
                    "  \"vat\": 91,\n" +
                    "  \"taxFreeAmount\": 0,\n" +
                    "  \"method\": \"카드\",\n" +
                    "  \"version\": \"2022-11-16\"\n" +
                    "}\n"
        )


    }

    private fun setUpTossPaymentFailResponse(): TossPayErrorResponse {
        mockServerUtils.addMockResponse(
            HttpStatus.BAD_REQUEST,
            "{\n" +
                    "  \"code\": \"NOT_FOUND_PAYMENT\",\n" +
                    "  \"message\": \"존재하지 않는 결제 입니다.\"\n" +
                    "}"
        )
        return TossPayErrorResponse(TossPayErrorCode.NOT_FOUND_PAYMENT, "존재하지 않는 결제 입니다.")
    }

    private fun savePerformance(performances: List<Performance>) {
        regionRepository.save(performances[0].performancePlace.region)
        placeRepository.save(performances[0].performancePlace)

        performances.forEach {
            performanceRepository.save(it)
        }
    }

    private fun createJwt(user: User): String {
        return jwtTokenProvider.sign(
            AuthenticatedUserDto.of(
                CustomUserDetailsDto(
                    user.uid,
                    user.email,
                    user.password,
                    user.nickname
                )
            ),
            mutableListOf()
        )
    }

    private fun createCarts(
        user: User,
        performanceDateTime: PerformanceDateTime,
        seats: List<PerformancePlaceSeat>
    ): List<Cart> {
        return seats.mapIndexed { idx, seat ->
            Cart("cart$idx", seat, performanceDateTime, user)
        }
    }
}