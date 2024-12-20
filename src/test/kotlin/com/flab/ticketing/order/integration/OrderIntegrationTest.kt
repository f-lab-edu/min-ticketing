package com.flab.ticketing.order.integration

import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.response.OrderDetailInfoResponse
import com.flab.ticketing.order.dto.response.OrderInfoResponse
import com.flab.ticketing.order.dto.response.OrderSummarySearchResult
import com.flab.ticketing.order.dto.service.TossPayConfirmResponse
import com.flab.ticketing.order.dto.service.TossPayErrorResponse
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.OrderMetaData
import com.flab.ticketing.order.enums.TossPayCancelErrorCode
import com.flab.ticketing.order.enums.TossPayConfirmErrorCode
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.OrderMetaDataRepository
import com.flab.ticketing.order.repository.OrderRepository
import com.flab.ticketing.order.service.client.TossPaymentClient.Companion.TOSS_EXCEPTION_PREFIX
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.testutils.IntegrationTest
import com.flab.ticketing.testutils.fixture.PerformanceFixture
import com.flab.ticketing.user.entity.User
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit


@Transactional
class OrderIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderMetaDataRepository: OrderMetaDataRepository

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    init {

        given("사용자의 장바구니 정보가 존재할 때") {
            val (user, jwt) = userPersistenceUtils.saveUserAndCreateJwt()

            val performance = performancePersistenceUtils.createAndSavePerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val performancePlace = performance.performancePlace

            val carts = orderPersistenceUtils.createAndSaveCarts(
                user = user,
                performanceDateTime = performanceDateTime,
                seats = performancePlace.seats.subList(0, 5)
            )

            `when`("장바구니를 선택하여 주문 정보를 생성할 시") {
                val uri = "/api/orders/toss/info"
                val orderCartUidList = carts.subList(0, 3).map { it.uid }
                val orderRequest = OrderInfoRequest(orderCartUidList)


                val mvcResult = mockMvc.perform(
                    post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest))
                )
                    .andDo(print())
                    .andReturn()

                then("주문 정보를 생성하여 저장 후 반환한다.") {
                    mvcResult.response.status shouldBe HttpStatus.CREATED.value()
                    val actual =
                        objectMapper.readValue(mvcResult.response.contentAsString, OrderInfoResponse::class.java)

                    val savedOrderMetaData = orderMetaDataRepository.findById(actual.orderId).orElseThrow()

                    actual.orderId shouldNotBe null
                    actual.amount shouldBe (performance.price * orderCartUidList.size)
                    savedOrderMetaData.amount shouldBeEqual actual.amount
                }
            }

        }

        given("사용자의 장바구니 정보가 존재할 때 - 잘못된 Cart UID 포함") {
            val (user, jwt) = userPersistenceUtils.saveUserAndCreateJwt()
            val performance = performancePersistenceUtils.createAndSavePerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 10)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val performancePlace = performance.performancePlace

            val carts = orderPersistenceUtils.createAndSaveCarts(
                user = user,
                performanceDateTime = performanceDateTime,
                seats = performancePlace.seats.subList(0, 5)
            )

            `when`("존재하지 않는 Cart UID로 주문 정보 생성 API 호출 시") {
                val uri = "/api/orders/toss/info"
                val orderCartUidList = carts.subList(0, 3).map { it.uid }.toMutableList()
                orderCartUidList.add("invalidCart001")

                val orderRequest = OrderInfoRequest(orderCartUidList)

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest))
                )
                    .andDo(print())
                    .andReturn()

                then("400 Bad Request를 출력한다.") {
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, OrderErrorInfos.INVALID_CART_INFO)
                }
            }
        }

        given("주문 생성이 완료되었을 때") {
            val (user, jwt) = userPersistenceUtils.saveUserAndCreateJwt()
            val performance = performancePersistenceUtils.createAndSavePerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 10)
            )
            val carts = orderPersistenceUtils.createAndSaveCarts(
                user = user,
                performanceDateTime = performance.performanceDateTime[0],
                seats = performance.performancePlace.seats.subList(0, 2)
            )

            orderMetaDataRepository.save(
                OrderMetaData(
                    "order-001",
                    performance.price * 2,
                    carts.map { it.uid },
                    user.uid
                )
            )

            val orderConfirmRequest = OrderConfirmRequest(
                paymentType = "카드",
                orderId = "order-001",
                paymentKey = "payment001",
                amount = performance.price * 2
            )

            val tossPayApiConfirmResp = setUpTossPaymentConfirmResponse(orderConfirmRequest)
            `when`("결제 승인을 요청할 시") {
                val uri = "/api/orders/toss/confirm"

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderConfirmRequest))
                )
                    .andDo(print())
                    .andReturn()

                then("토스 결제 승인 API를 호출하고, Order를 저장한다.") {
                    mvcResult.response.status shouldBe HttpStatus.OK.value()
                    val actual = orderRepository.findByUid("order-001")!!

                    actual.user shouldBeEqual user
                    actual.reservations.size shouldBe 2
                    actual.payment.paymentKey shouldBeEqual tossPayApiConfirmResp!!.paymentKey
                    actual.status shouldBe Order.OrderStatus.COMPLETED
                }
            }
        }

        given("주문 생성이 완료 되었을 때 - 잘못된 유저 요청") {
            val user = userPersistenceUtils.saveNewUser()
            val performance = performancePersistenceUtils.createAndSavePerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 10)
            )

            val carts = orderPersistenceUtils.createAndSaveCarts(
                user = user,
                performanceDateTime = performance.performanceDateTime[0],
                seats = performance.performancePlace.seats.subList(0, 2)
            )

            orderMetaDataRepository.save(
                OrderMetaData(
                    "order-001",
                    performance.price * 2,
                    carts.map { it.uid },
                    user.uid
                )
            )

            val orderConfirmRequest = OrderConfirmRequest(
                paymentType = "카드",
                orderId = "order-001",
                paymentKey = "payment001",
                amount = performance.price * 2
            )

            `when`("다른 유저로 Order 주문 확정 API 호출 시") {
                val (_, jwt) = userPersistenceUtils.saveUserAndCreateJwt(
                    uid = "user002",
                    email = "email2@email.com"
                )

                val uri = "/api/orders/toss/confirm"

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderConfirmRequest))
                )
                    .andDo(print())
                    .andReturn()

                then("403 BAD Request와 적절한 오류 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.FORBIDDEN, OrderErrorInfos.INVALID_USER)
                }
            }
        }

        given("주문 생성이 완료 되었을 때 - 토스 페이 API 정상 응답이 아닌 경우") {
            val (user, jwt) = userPersistenceUtils.saveUserAndCreateJwt()
            val performance = performancePersistenceUtils.createAndSavePerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 10)
            )
            val carts = orderPersistenceUtils.createAndSaveCarts(
                user = user,
                performanceDateTime = performance.performanceDateTime[0],
                seats = performance.performancePlace.seats.subList(0, 2)
            )

            orderMetaDataRepository.save(
                OrderMetaData(
                    "order-001",
                    performance.price * 2,
                    carts.map { it.uid },
                    user.uid
                )
            )

            val orderConfirmRequest = OrderConfirmRequest(
                paymentType = "카드",
                orderId = "order-001",
                paymentKey = "payment001",
                amount = performance.price * 2
            )

            val tossPayResponse = setUpTossPaymentFailConfirmResponse()

            `when`("결제 승인 API를 호출 시") {
                val uri = "/api/orders/toss/confirm"

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderConfirmRequest))
                )
                    .andDo(print())
                    .andReturn()

                then("토스 페이 API의 응답 정보를 반환하고, 주문 확정 작업을 롤백한다.") {
                    checkError(
                        mvcResult,
                        HttpStatus.NOT_FOUND,
                        CommonErrorInfos.EXTERNAL_API_ERROR.code,
                        TOSS_EXCEPTION_PREFIX + tossPayResponse.message
                    )
                    orderRepository.findByUid("order-001") shouldBe null
                }
            }

        }

        given("사용자의 주문 정보가 존재할 때") {
            val (user, jwt) = userPersistenceUtils.saveUserAndCreateJwt()
            val performances = PerformanceFixture.createPerformanceGroupbyRegion(
                performanceCount = 2,
                seatPerPlace = 5
            )
            performancePersistenceUtils.savePerformances(performances)


            val order1 = orderPersistenceUtils.createAndSaveOrder(
                user = user,
                performanceDateTime = performances[0].performanceDateTime[0],
                seats = listOf(performances[0].performancePlace.seats[0])
            )
            val order2 = orderPersistenceUtils.createAndSaveOrder(
                user = user,
                performanceDateTime = performances[1].performanceDateTime[0],
                seats = listOf(performances[1].performancePlace.seats[0])
            )
            val order3 = orderPersistenceUtils.createAndSaveOrder(
                user = user,
                performanceDateTime = performances[1].performanceDateTime[1],
                seats = listOf(performances[1].performancePlace.seats[1])
            )


            `when`("주문 정보 리스트 조회시") {
                val uri = "/api/orders"
                val mvcResult = mockMvc.perform(
                    get(uri)
                        .param("limit", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(print())
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

        given("아직 공연이 시작되지 않은 사용자의 확정된 주문 정보가 존재할 때") {
            val (user, jwt) = userPersistenceUtils.saveUserAndCreateJwt()
            val performance = performancePersistenceUtils.createAndSavePerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(10)
            )

            val order = orderPersistenceUtils.createAndSaveOrder(
                user = user,
                performanceDateTime = performance.performanceDateTime[0],
                seats = listOf(performance.performancePlace.seats[0]),
            )

            setUpTossPaymentCancelResponse()

            `when`("주문 취소를 시도할 시") {
                val uri = "/api/orders/${order.uid}/cancel"

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(print())
                    .andReturn()

                then("주문의 상태를 CANCEL로 바꾸고 200 코드를 반환한다.") {
                    mvcResult.response.status shouldBe HttpStatus.OK.value()

                    orderRepository.findByUid(order.uid)!!.status shouldBe Order.OrderStatus.CANCELED
                }
            }

        }

        given("아직 공연이 시작되지 않은 사용자의 확정된 주문 정보가 존재할 때 - Toss API 오류") {
            val (user, jwt) = userPersistenceUtils.saveUserAndCreateJwt()
            val performance = performancePersistenceUtils.createAndSavePerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(10)
            )
            val order = orderPersistenceUtils.createAndSaveOrder(
                user = user,
                performanceDateTime = performance.performanceDateTime[0],
                seats = listOf(performance.performancePlace.seats[0]),
            )

            val tossErrorResponse = setUpTossPaymentFailCancelResponse()

            `when`("주문 취소를 시도할 시") {
                val uri = "/api/orders/${order.uid}/cancel"

                val mvcResult = mockMvc.perform(
                    post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(print())
                    .andReturn()

                then("토스 페이먼츠 오류에 따른 적절한 상태코드와 메시지를 반환한다.") {
                    checkError(
                        mvcResult,
                        tossErrorResponse.code.responseStatus,
                        CommonErrorInfos.EXTERNAL_API_ERROR.code,
                        TOSS_EXCEPTION_PREFIX + tossErrorResponse.message
                    )
                }
            }


        }


        given("주문의 상태가 CANCELED인 주문이 존재할 때") {
            val (user, jwt) = userPersistenceUtils.saveUserAndCreateJwt()
            val performance = performancePersistenceUtils.createAndSavePerformance()

            orderPersistenceUtils.createAndSaveOrder(
                user = user,
                performanceDateTime = performance.performanceDateTime[0],
                seats = listOf(performance.performancePlace.seats[0]),
            )

            val canceledOrder = orderPersistenceUtils.createAndSaveOrder(
                user = user,
                performanceDateTime = performance.performanceDateTime[0],
                seats = listOf(performance.performancePlace.seats[0]),
                orderStatus = Order.OrderStatus.CANCELED
            )


            `when`("주문이 CANCELED 상태인 주문을 조회할 시") {
                val uri = "/api/orders"

                val mvcResult = mockMvc.perform(
                    get(uri)
                        .param("status", Order.OrderStatus.CANCELED.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                ).andDo(
                    print()
                ).andReturn()

                then("CANCELED 상태의 주문을 반환한다.") {
                    val actual = objectMapper.readValue<CursoredResponse<OrderSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            OrderSummarySearchResult::class.java
                        )
                    )

                    actual.data.size shouldBe 1
                    actual.data[0].uid shouldBe canceledOrder.uid
                }
            }

        }


        given("사용자의 주문 정보가 존재할 때 - 상세 조회") {
            val (user, jwt) = userPersistenceUtils.saveUserAndCreateJwt()
            val performance = performancePersistenceUtils.createAndSavePerformance(
                place = PerformanceFixture.createPerformancePlace(numSeats = 5)
            )

            val seats = performance.performancePlace.seats
            val order = orderPersistenceUtils.createAndSaveOrder(
                user = user,
                performanceDateTime = performance.performanceDateTime[0],
                seats = listOf(seats[0], seats[1])
            )

            `when`("사용자가 주문 상세 조회 시도시") {
                val url = "/api/orders/${order.uid}"

                val mvcResult = mockMvc.perform(
                    get(url).header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                ).andDo(
                    print()
                ).andReturn()

                then("상세 주문 정보를 반환한다.") {
                    mvcResult.response.status shouldBe 200
                    val (orderUid, totalPrice, orderName, orderStatus, orderedAt, paymentMethod, reservations) = objectMapper.readValue(
                        mvcResult.response.contentAsString,
                        OrderDetailInfoResponse::class.java
                    )

                    orderUid shouldBe order.uid
                    totalPrice shouldBe order.payment.totalPrice
                    orderStatus shouldBe order.status
                    orderName shouldBeEqual order.name
                    orderedAt.truncatedTo(ChronoUnit.MILLIS) shouldBeEqual
                            order.createdAt.withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)

                    paymentMethod shouldBe order.payment.paymentMethod
                    reservations shouldContainAll List(size = 2) {
                        OrderDetailInfoResponse.ReservationDetailInfo(
                            performance.name,
                            performance.price,
                            order.reservations[it].qrImageUrl!!,
                            false
                        )
                    }

                }
            }
        }
    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)

        PerformanceFixture.reset()
        withContext(Dispatchers.IO) {
            orderPersistenceUtils.clearContext()
            userPersistenceUtils.clearContext()
            performancePersistenceUtils.clearContext()
            orderMetaDataRepository.deleteAll()
            redisTemplate.connectionFactory?.connection?.flushAll()
        }
    }

    private fun setUpTossPaymentConfirmResponse(orderConfirmRequest: OrderConfirmRequest): TossPayConfirmResponse? {
        val body = """
            {
              "mId": "tosspayments",
              "lastTransactionKey": "9C62B18EEF0DE3EB7F4422EB6D14BC6E",
              "paymentKey": "${orderConfirmRequest.paymentKey}",
              "orderId": "${orderConfirmRequest.orderId}",
              "orderName": "토스 티셔츠 외 2건",
              "taxExemptionAmount": 0,
              "status": "DONE",
              "requestedAt": "2024-02-13T12:17:57+09:00",
              "approvedAt": "2024-02-13T12:18:14+09:00",
              "useEscrow": false,
              "cultureExpense": false,
              "card": {
                "issuerCode": "71",
                "acquirerCode": "71",
                "number": "12345678****000*",
                "installmentPlanMonths": 0,
                "isInterestFree": false,
                "interestPayer": null,
                "approveNo": "00000000",
                "useCardPoint": false,
                "cardType": "신용",
                "ownerType": "개인",
                "acquireStatus": "READY",
                "receiptUrl": "https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20240213121757MvuS8&ref=PX",
                "amount": 1000
              },
              "virtualAccount": null,
              "transfer": null,
              "mobilePhone": null,
              "giftCertificate": null,
              "cashReceipt": null,
              "cashReceipts": null,
              "discount": null,
              "cancels": null,
              "secret": null,
              "type": "NORMAL",
              "easyPay": {
                "provider": "토스페이",
                "amount": 0,
                "discountAmount": 0
              },
              "easyPayAmount": 0,
              "easyPayDiscountAmount": 0,
              "country": "KR",
              "failure": null,
              "isPartialCancelable": true,
              "receipt": {
                "url": "https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20240213121757MvuS8&ref=PX"
              },
              "checkout": {
                "url": "https://api.tosspayments.com/v1/payments/5EnNZRJGvaBX7zk2yd8ydw26XvwXkLrx9POLqKQjmAw4b0e1/checkout"
              },
              "currency": "KRW",
              "totalAmount": ${orderConfirmRequest.amount},
              "balanceAmount": 1000,
              "suppliedAmount": 909,
              "vat": 91,
              "taxFreeAmount": 0,
              "method": "카드",
              "version": "2022-11-16"
            }
        """.trimIndent()

        mockServerUtils.addMockResponse(
            HttpStatus.OK,
            body
        )

        return objectMapper.readValue(body, TossPayConfirmResponse::class.java)
    }

    private fun setUpTossPaymentFailConfirmResponse(): TossPayErrorResponse {
        val errorResponse = mapOf(Pair("code", "NOT_FOUND_PAYMENT"), Pair("message", "존재하지 않는 결제 입니다."))

        setUpTossPaymentFailResponse(HttpStatus.BAD_REQUEST, errorResponse)
        return TossPayErrorResponse(TossPayConfirmErrorCode.NOT_FOUND_PAYMENT, "존재하지 않는 결제 입니다.")

    }

    private fun setUpTossPaymentCancelResponse() {
        val body = """
            {
              "mId": "tosspayments",
              "version": "2022-11-16",
              "lastTransactionKey": "GOtuE_rpkelaDwxW_ULZj",
              "paymentKey": "Zrmyj5eoBkWaVldAqO-i6",
              "orderId": "frEwn_vLxqeg3l5MhCsph",
              "orderName": "토스 티셔츠 외 2건",
              "currency": "KRW",
              "method": "카드",
              "status": "CANCELED",
        
              "cancels": [
                {
                  "cancelReason": "고객이 취소를 원함",
                  "canceledAt": "2022-01-01T11:32:04+09:00",
                  "cancelAmount": 10000,
                  "taxFreeAmount": 0,
                  "taxExemptionAmount": 0,
                  "refundableAmount": 0,
                  "easyPayDiscountAmount": 0,
                  "transactionKey": "8B4F646A829571D870A3011A4E13D640",
                  "receiptKey": "V4AJ6AhSWsGN0RocizZQlagPLN8s2IahJLXpfSHzQBTKoDG7",
                  "cancelStatus": "DONE",
                  "cancelRequestId": null
                }
              ],
              "secret": null,
              "type": "NORMAL",
              "easyPay": "토스페이",
              "country": "KR",
              "failure": null,
              "totalAmount": 10000,
              "balanceAmount": 0,
              "suppliedAmount": 0,
              "vat": 0,
              "taxFreeAmount": 0,
              "taxExemptionAmount": 0
            }
        """.trimIndent()

        mockServerUtils.addMockResponse(HttpStatus.OK, body)
    }

    private fun setUpTossPaymentFailCancelResponse(): TossPayErrorResponse {
        val errorResponse = mapOf(Pair("code", "ALREADY_CANCELED_PAYMENT"), Pair("message", "이미 취소된 결제 입니다."))
        setUpTossPaymentFailResponse(HttpStatus.BAD_REQUEST, errorResponse)

        return TossPayErrorResponse(TossPayCancelErrorCode.ALREADY_CANCELED_PAYMENT, "이미 취소된 결제 입니다.")
    }


    private fun setUpTossPaymentFailResponse(expectStatus: HttpStatus, resp: Map<String, String>) {
        val body = objectMapper.writeValueAsString(resp)
        mockServerUtils.addMockResponse(expectStatus, body)
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