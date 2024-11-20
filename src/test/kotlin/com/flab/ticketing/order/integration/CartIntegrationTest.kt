package com.flab.ticketing.order.integration

import com.flab.ticketing.order.dto.response.CartListResponse
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.order.repository.OrderRepository
import com.flab.ticketing.testutils.IntegrationTest
import com.flab.ticketing.testutils.generator.PerformanceTestDataGenerator
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import java.time.ZoneOffset
import java.time.ZonedDateTime

class CartIntegrationTest : IntegrationTest() {
    
    @Autowired
    private lateinit var cartRepository: CartRepository

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    init {

        given("시작하지 않은 공연 정보가 존재할 때") {
            val performance = performanceTestUtils.createAndSavePerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(1)
            )

            val performanceDateTime = performance.performanceDateTime[0]
            val place = performance.performancePlace

            val (user, jwt) = userTestUtils.saveUserAndCreateJwt()


            `when`("로그인된 사용자가 에약되지 않은 좌석에 예약을 시도할 시") {
                val reserveSeat = place.seats[0]

                val uri =
                    "/api/reservations/${performance.uid}/dates/${performanceDateTime.uid}/seats/${reserveSeat.uid}"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("201 CREATED를 반환하고 DB에 예약 정보를 저장한다.") {
                    mvcResult.response.status shouldBe HttpStatus.CREATED.value()

                    val actual = cartRepository.findByDateUidAndSeatUid(performanceDateTime.uid, reserveSeat.uid)

                    actual!!.user shouldBeEqual user

                    actual.performanceDateTime shouldBeEqual performanceDateTime
                    actual.seat shouldBeEqual reserveSeat
                }
            }
        }

        given("시작하지 않은 공연 정보가 존재할 때 - 카트 중복") {
            val performance = performanceTestUtils.createAndSavePerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(1)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val place = performance.performancePlace
            val reservedSeat = place.seats[0]

            val (user, jwt) = userTestUtils.saveUserAndCreateJwt()

            orderTestUtils.createAndSaveCarts(
                user = user,
                performanceDateTime = performanceDateTime,
                seats = listOf(reservedSeat)
            )

            `when`("이미 카트에 존재하는 좌석을 예매할 시") {
                val uri =
                    "/api/reservations/${performance.uid}/dates/${performanceDateTime.uid}/seats/${reservedSeat.uid}"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()
                then("409 conflict 오류를 throw한다.") {
                    checkError(mvcResult, HttpStatus.CONFLICT, OrderErrorInfos.ALREADY_RESERVED)
                }
            }
        }

        given("시작하지 않은 공연 정보가 존재할 때 - 예약 중복") {
            val performance = performanceTestUtils.createAndSavePerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(1)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val place = performance.performancePlace
            val reservedSeat = place.seats[0]

            val (user, jwt) = userTestUtils.saveUserAndCreateJwt()

            // 이미 존재하는 예약
            orderTestUtils.createAndSaveOrder(
                user = user,
                performanceDateTime = performanceDateTime,
                seats = place.seats.subList(0, 1)
            )

            `when`("이미 카트에 존재하는 좌석을 예매할 시") {
                val uri =
                    "/api/reservations/${performance.uid}/dates/${performanceDateTime.uid}/seats/${reservedSeat.uid}"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.post(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()
                then("409 conflict 오류를 throw한다.") {
                    checkError(mvcResult, HttpStatus.CONFLICT, OrderErrorInfos.ALREADY_RESERVED)
                }
            }
        }

        given("장바구니의 요소가 존재할 때") {
            val (user, jwt) = userTestUtils.saveUserAndCreateJwt()

            val performance = performanceTestUtils.createAndSavePerformance()
            val performanceDateTime = performance.performanceDateTime[0]

            val carts = orderTestUtils.createAndSaveCarts(
                user = user,
                performanceDateTime = performanceDateTime,
                seats = performance.performancePlace.seats.subList(0, 2)
            )

            `when`("장바구니를 조회할 시") {
                val uri = "/api/orders/carts"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("자신이 담은 장바구니를 반환한다.") {
                    val actual =
                        objectMapper.readValue(mvcResult.response.contentAsString, CartListResponse::class.java)
                    val expected = CartListResponse(
                        carts.map {
                            CartListResponse.CartInfo(
                                it.uid,
                                performanceDateTime.showTime.withZoneSameInstant(ZoneOffset.UTC),
                                performance.name,
                                performance.price,
                                it.seat.name
                            )
                        }
                    )

                    mvcResult.response.status shouldBe HttpStatus.OK.value()
                    actual.data shouldContainAll expected.data
                }
            }
        }

    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)

        PerformanceTestDataGenerator.reset()
        withContext(Dispatchers.IO) {
            orderTestUtils.clearContext()
            userTestUtils.clearContext()
            performanceTestUtils.clearContext()
            redisTemplate.connectionFactory?.connection?.flushAll()
        }


    }


}