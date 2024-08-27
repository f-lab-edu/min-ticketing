package com.flab.ticketing.order.integration

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.OrderTestDataGenerator
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UserTestDataGenerator
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.response.OrderInfoResponse
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.order.repository.OrderRepository
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

                then("주문 정보를 생성하여 반환하며, DB에 임시 주문 정보를 저장한다.") {
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

                then("토스 결제 승인 API를 호출하고, Order의 상태를 COMPLETED로 바꾸며, Cart 정보를 삭제한다.") {
                    mvcResult.response.status shouldBe HttpStatus.OK.value()
                    orderRepository.findByUid(order.uid)!!.status shouldBe Order.OrderStatus.COMPLETED
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