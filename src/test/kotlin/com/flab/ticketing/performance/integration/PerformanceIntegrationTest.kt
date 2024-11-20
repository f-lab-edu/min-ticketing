package com.flab.ticketing.performance.integration

import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.common.dto.response.ListedResponse
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.order.repository.OrderRepository
import com.flab.ticketing.order.repository.ReservationRepository
import com.flab.ticketing.performance.dto.response.PerformanceDateDetailResponse
import com.flab.ticketing.performance.dto.response.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.response.RegionInfoResponse
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlace
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.testutils.IntegrationTest
import com.flab.ticketing.testutils.generator.OrderTestDataGenerator
import com.flab.ticketing.testutils.generator.PerformanceTestDataGenerator
import com.flab.ticketing.user.entity.User
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PerformanceIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Autowired
    private lateinit var cartRepository: CartRepository

    init {

        given("공연 정보가 존재할 때 - 상세 조회 검색") {
            val user = userTestUtils.saveNewUser()
            val performance = performanceTestUtils.createAndSavePerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )
            val place = performance.performancePlace

            val carts = createCarts(
                user,
                performance.performanceDateTime[0],
                performance.performancePlace.seats.subList(0, 3)
            )
            cartRepository.saveAll(carts)

            `when`("특정 공연의 UID를 가지고 공연 상세 정보를 검색할 시") {
                val uri = "/api/performances/${performance.uid}"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("해당 공연의 상세 정보를 반환한다.") {
                    val actual = objectMapper.readValue(
                        mvcResult.response.contentAsString,
                        PerformanceDetailResponse::class.java
                    )

                    val expectedDateInfo = performance.performanceDateTime.map {
                        PerformanceDetailResponse.DateInfo(
                            uid = it.uid,
                            dateTime = it.showTime.toLocalDateTime(),
                            total = place.seats.size.toLong(),
                            remaining = place.seats.size.toLong() - carts.size
                        )
                    }

                    val expected = createDetailSearchExpected(performance, expectedDateInfo)

                    mvcResult.response.status shouldBe HttpStatus.OK.value()
                    actual shouldBeEqual expected
                }

            }

        }

        given("공연 정보가 존재하지 않을 때") {

            `when`("존재하지 않는 공연의 UID로 상세 조회 시") {
                val invalidUid = "asdasdsa"
                val uri = "/api/performances/$invalidUid"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("404 상태 코드와 알맞은 메시지를 반환한다.") {
                    checkError(mvcResult, HttpStatus.NOT_FOUND, PerformanceErrorInfos.PERFORMANCE_NOT_FOUND)
                }
            }
        }

        given("공연 정보가 존재할 때") {
            val place = PerformancePlace(PerformanceTestDataGenerator.createRegion(), "장소")
            val seatRowColumn = listOf(
                1 to 1,
                1 to 2,
                2 to 1,
                2 to 2,
                3 to 1
            )

            seatRowColumn.forEachIndexed { index, (row, col) ->
                place.addSeat("seat$index", row, col)
            }

            val (user, jwt) = userTestUtils.saveUserAndCreateJwt()

            val performance = performanceTestUtils.createAndSavePerformance(
                place = place,
                numShowtimes = 2,
                showTimeStartDateTime = ZonedDateTime.now().plusDays(1)
            )
            val performanceDate = performance.performanceDateTime[0]


            val order = OrderTestDataGenerator.createOrder(
                user = user
            )

            OrderTestDataGenerator.createReservations(
                performanceDate,
                performance.performancePlace.seats.subList(0, 3),
                order
            )
            val carts = createCarts(user, performanceDate, performance.performancePlace.seats.subList(3, 4))

            saveOrder(order)
            cartRepository.saveAll(carts)


            `when`("로그인한 유저가 공연 날짜의 좌석 정보를 조회할 시") {
                val uri = "/api/performances/${performance.uid}/dates/${performanceDate.uid}"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("해당 공연 날짜의 좌석 정보들을 반환한다.") {
                    val expected = PerformanceDateDetailResponse(
                        performanceDate.uid,
                        performance.price,
                        listOf(
                            listOf(
                                PerformanceDateDetailResponse.SeatInfo(
                                    place.seats[0].uid,
                                    place.seats[0].name,
                                    true
                                ),
                                PerformanceDateDetailResponse.SeatInfo(
                                    place.seats[1].uid,
                                    place.seats[1].name,
                                    true
                                )
                            ),
                            listOf(
                                PerformanceDateDetailResponse.SeatInfo(
                                    place.seats[2].uid,
                                    place.seats[2].name,
                                    true
                                ),
                                PerformanceDateDetailResponse.SeatInfo(
                                    place.seats[3].uid,
                                    place.seats[3].name,
                                    true
                                )
                            ),
                            listOf(
                                PerformanceDateDetailResponse.SeatInfo(
                                    place.seats[4].uid,
                                    place.seats[4].name,
                                    false
                                )
                            )
                        )
                    )

                    mvcResult.response.status shouldBe HttpStatus.OK.value()
                    val actual = objectMapper.readValue(
                        mvcResult.response.contentAsString,
                        PerformanceDateDetailResponse::class.java
                    )

                    actual.dateUid shouldBeEqual expected.dateUid
                    actual.pricePerSeat shouldBeEqual expected.pricePerSeat
                    for (i in 0..<actual.seats.size) {
                        actual.seats[i] shouldContainExactly expected.seats[i]
                    }
                }
            }
        }
        given("공연 날짜 정보가 존재하지 않을 때") {

            val (_, jwt) = userTestUtils.saveUserAndCreateJwt()

            `when`("로그인한 유저가 잘못된 공연과, 공연 날짜 정보로 좌석 정보를 조회할 시") {
                val invalidPerformanceId = "Per"
                val invalidDateId = "date"

                val uri = "/api/performances/$invalidPerformanceId/dates/$invalidDateId"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()
                then("Not Found 상태 코드를 반환한다.") {
                    checkError(mvcResult, HttpStatus.NOT_FOUND, PerformanceErrorInfos.PERFORMANCE_NOT_FOUND)
                }
            }
        }
        given("공연 정보가 존재할 때 - 잘못된 날짜 고유 식별자") {
            val performance = performanceTestUtils.createAndSavePerformance()
            val (_, jwt) = userTestUtils.saveUserAndCreateJwt()

            `when`("공연 UID는 올바르나, 공연 날짜 UID가 공연에 속하지 않은 경우") {
                val performanceUid = performance.uid
                val invalidDateUid = "invalid"
                val uri = "/api/performances/$performanceUid/dates/$invalidDateUid"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("404 Not Found 오류를 반환한다.") {
                    checkError(mvcResult, HttpStatus.NOT_FOUND, PerformanceErrorInfos.PERFORMANCE_DATE_NOT_FOUND)
                }
            }
        }


        given("이미 지난 공연 정보가 존재할 때") {

            val performance = performanceTestUtils.createAndSavePerformance(
                showTimeStartDateTime = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.of("Asia/Seoul")),
                numShowtimes = 1
            )

            val performanceDateTime = performance.performanceDateTime[0]

            val (_, jwt) = userTestUtils.saveUserAndCreateJwt()

            `when`("로그인한 사용자가 이미 지난 공연의 좌석 정보를 조회할 시") {
                val uri = "/api/performances/${performance.uid}/dates/${performanceDateTime.uid}"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("400 BAD Request를 반환한다.") {
                    checkError(mvcResult, HttpStatus.BAD_REQUEST, PerformanceErrorInfos.PERFORMANCE_ALREADY_PASSED)
                }
            }
        }


        given("공연 정보가 6개 이상 존재할 때 - v2") {
            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 6
            )
            performanceTestUtils.savePerformances(performances)

            `when`("사용자가 5개의 공연 정보를 조회할 시") {
                val uri = "/api/performances"
                val limit = 5

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", limit.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("정렬조건은 최신 등록 순으로, 5개의 공연 정보(1페이지)와 다음 공연의 커서를 받을 수 있다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(performances.drop(1))

                    actual.cursor?.shouldBeEqual(performances[0].uid)
                    actual.data.size shouldBe 5
                    actual.data shouldContainExactly expected
                }
            }
        }


        given("지역 정보가 존재할 때") {
            val regionsList = List(5) {
                PerformanceTestDataGenerator.createRegion("region$it")
            }

            performanceTestUtils.saveRegions(regionsList)

            `when`("지역을 조회할 시") {
                val uri = "/api/performances/regions"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("현재 존재하는 지역의 모든 리스트를 조회할 수 있다.") {
                    val actual = objectMapper.readValue<ListedResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            ListedResponse::class.java,
                            RegionInfoResponse::class.java
                        )
                    )

                    actual.data shouldContainAll regionsList.map { RegionInfoResponse(it.uid, it.name) }

                }
            }
        }
    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)

        PerformanceTestDataGenerator.reset()
        withContext(Dispatchers.IO) {
            cartRepository.deleteAll()
            reservationRepository.deleteAll()
            orderRepository.deleteAll()
            userTestUtils.clearContext()
            performanceTestUtils.clearContext()
        }


    }

    private fun saveOrder(order: Order) {
        orderRepository.save(order)
        reservationRepository.saveAll(order.reservations)
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

    private fun createSearchExpectedOrderByIdDesc(performances: List<Performance>): List<PerformanceSummarySearchResult> {
        val sorted = performances.sortedBy { it.id }.asReversed()

        return sorted.map {
            PerformanceSummarySearchResult(
                it.uid,
                it.image,
                it.name,
                it.performancePlace.region.name,
                it.performanceDateTime.minOf { d -> d.showTime },
                it.performanceDateTime.maxOf { d -> d.showTime }
            )
        }
    }

    private fun createDetailSearchExpected(
        performance: Performance,
        dateInfos: List<PerformanceDetailResponse.DateInfo>
    ): PerformanceDetailResponse {

        return PerformanceDetailResponse(
            uid = performance.uid,
            image = performance.image,
            title = performance.name,
            region = performance.performancePlace.region.name,
            place = performance.performancePlace.name,
            price = performance.price,
            description = performance.description,
            dateInfo = dateInfos
        )
    }

}