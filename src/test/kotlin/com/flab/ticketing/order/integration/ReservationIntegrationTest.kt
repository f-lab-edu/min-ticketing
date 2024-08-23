package com.flab.ticketing.order.integration

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.OrderTestDataGenerator
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UserTestDataGenerator
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.order.repository.OrderRepository
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.UserRepository
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import java.time.ZonedDateTime

class ReservationIntegrationTest : IntegrationTest() {

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

    @Autowired
    private lateinit var cartRepository: CartRepository

    init {

        given("공연 정보가 존재할 때") {
            val performance = PerformanceTestDataGenerator.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(1)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val place = performance.performancePlace

            val user = UserTestDataGenerator.createUser()

            savePerformance(listOf(performance))
            userRepository.save(user)

            val jwt = createJwt(user)
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
                    actual.dateUid shouldBeEqual performanceDateTime.uid
                    actual.seatUid shouldBeEqual reserveSeat.uid
                }
            }
        }

        given("공연 정보가 존재할 때 - 카트 중복") {
            val performance = PerformanceTestDataGenerator.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(1)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val place = performance.performancePlace
            val reservedSeat = place.seats[0]

            val user = UserTestDataGenerator.createUser()

            savePerformance(listOf(performance))
            userRepository.save(user)
            cartRepository.save(Cart(reservedSeat.uid, performanceDateTime.uid, user))

            val jwt = createJwt(user)
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

        given("공연 정보가 존재할 때 - 예약 중복") {
            val performance = PerformanceTestDataGenerator.createPerformance(
                showTimeStartDateTime = ZonedDateTime.now().plusDays(1)
            )
            val performanceDateTime = performance.performanceDateTime[0]
            val place = performance.performancePlace
            val reservedSeat = place.seats[0]

            val user = UserTestDataGenerator.createUser()

            val reservations = OrderTestDataGenerator.createReservations(
                performanceDateTime,
                place.seats.subList(0, 1),
                OrderTestDataGenerator.createOrder(user = user)
            )

            savePerformance(listOf(performance))
            userRepository.save(user)
            saveOrder(reservations[0].order)

            val jwt = createJwt(user)
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

    private fun saveOrder(order: Order) {
        orderRepository.save(order)
    }

}