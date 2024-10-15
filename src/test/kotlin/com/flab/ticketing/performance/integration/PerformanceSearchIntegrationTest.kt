package com.flab.ticketing.performance.integration

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.dto.service.CustomUserDetailsDto
import com.flab.ticketing.auth.utils.JwtTokenProvider
import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.OrderTestDataGenerator
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UserTestDataGenerator
import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.order.repository.OrderRepository
import com.flab.ticketing.order.repository.ReservationRepository
import com.flab.ticketing.performance.dto.response.PerformanceDateDetailResponse
import com.flab.ticketing.performance.dto.response.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlace
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.UserRepository
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
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

class PerformanceSearchIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var regionRepository: RegionRepository

    @Autowired
    private lateinit var placeRepository: PerformancePlaceRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var cartRepository: CartRepository

    init {

        given("공연 정보가 6개 이상 존재할 때") {

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 6
            )

            savePerformance(performances)

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

        given("공연 정보가 5개 이하로 존재할 시") {

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 5
            )
            savePerformance(performances)

            `when`("사용자가 5개의 공연 정보를 조회할 시") {
                val uri = "/api/performances"
                val limit = 5

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", limit.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("cursor 정보는 NULL이고, 공연 정보가 존재하는 만큼 반환한다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(performances)

                    actual.cursor shouldBe null
                    actual.data.size shouldBe 5
                    actual.data shouldContainExactly expected

                }
            }
        }

        given("공연 정보가 존재할 때 - 지역 검색") {

            val seoulRegionPerformances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                regionName = "서울",
                performanceCount = 3
            )

            val gumiPerformanceCount = 3

            val gumiRegionPerformances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                regionName = "구미",
                performanceCount = gumiPerformanceCount
            )

            savePerformance(seoulRegionPerformances)
            savePerformance(gumiRegionPerformances)

            `when`("특정 지역으로 공연을 검색할 시") {
                val uri = "/api/performances"
                val gumiRegionUid = gumiRegionPerformances[0].performancePlace.region.uid

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("region", gumiRegionUid)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("필터링 된 공연 정보 리스트가 반환된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(gumiRegionPerformances)

                    actual.cursor shouldBe null
                    actual.data.size shouldBe gumiPerformanceCount
                    actual.data shouldContainExactly expected
                }
            }
        }
        given("공연 정보가 존재할 때 - 최저 금액 검색") {
            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performancePrices = listOf(2000, 3000, 4000)

            val performances = PerformanceTestDataGenerator.createPerformancesPriceIn(
                place = place,
                priceIn = performancePrices
            )
            savePerformance(performances)

            `when`("최저으로 공연을 검색할 시") {
                val uri = "/api/performances"
                val minPrice = 3000

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("minPrice", minPrice.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("특정 금액 이상의 공연만 검색된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(performances.drop(1))

                    actual.data.size shouldBe expected.size
                    actual.data shouldContainExactly expected
                }
            }
        }
        given("공연 정보가 존재할 때 - 최고 금액 검색") {

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performancePrices = listOf(2000, 3000, 4000)

            val performances = PerformanceTestDataGenerator.createPerformancesPriceIn(
                place = place,
                priceIn = performancePrices
            )
            savePerformance(performances)

            `when`("최고 금액으로 공연을 검색할 시") {
                val uri = "/api/performances"
                val maxPrice = 3000

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("maxPrice", maxPrice.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("특정 금액 이하의 공연만 검색된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(performances.dropLast(1))

                    actual.data.size shouldBe expected.size
                    actual.data shouldContainExactly expected
                }
            }

        }

        given("공연 정보가 존재할 때 - 공연 날짜 검색") {

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performance1DateTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )
            val performance2DateTime = ZonedDateTime.of(
                LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )

            val performances = PerformanceTestDataGenerator.createPerformancesDatesIn(
                place = place,
                dateIn = listOf(performance1DateTime, performance2DateTime)
            )

            savePerformance(performances)


            `when`("특정 공연 날짜로 검색할 시") {
                val uri = "/api/performances"
                val searchDate = performance1DateTime

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("showTime", searchDate.toString())
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()



                then("특정 공연날짜가 포함된 공연만 반환한다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(listOf(performances[0]))

                    actual.cursor shouldBe null
                    actual.data shouldContainExactly expected
                }
            }
        }

        given("공연 정보가 존재할 때 - 공연 이름 검색") {
            val givenNames = listOf("멋진 공연", "예쁜 공연", "아주 멋진 공연")

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performances = PerformanceTestDataGenerator.createPerformancesInNames(
                place = place,
                nameIn = givenNames
            )
            savePerformance(performances)

            `when`("공연의 이름으로 검색할 시") {
                val uri = "/api/performances"
                val nameQuery = "멋진"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("q", nameQuery)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()


                then("검색 공연 이름이 포함된 공연만 조회된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(listOf(performances[0], performances[2]))

                    actual.cursor shouldBe null
                    actual.data shouldContainExactly expected

                }
            }
        }
        given("공연 정보가 존재할 때 - 멀티 조건 검색") {

            val region = PerformanceTestDataGenerator.createRegion()
            val place = PerformanceTestDataGenerator.createPerformancePlace(region)

            val performance1DateTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 1, 1, 10, 0, 0),
                ZoneId.of("Asia/Seoul")
            )
            val performance1Price = 50000
            val performance1Name = "공공공연"

            val performance1 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = performance1DateTime,
                price = performance1Price,
                name = performance1Name
            )

            val performance2 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = performance1DateTime,
                price = 10000,
                name = performance1Name
            )

            val performance3 = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimeStartDateTime = ZonedDateTime.of(
                    LocalDateTime.of(2023, 1, 1, 10, 0, 0),
                    ZoneId.of("Asia/Seoul")
                ),
                price = 15000
            )

            savePerformance(listOf(performance1, performance2, performance3))

            `when`("다양한 조건으로 공연을 검색할 시") {
                val uri = "/api/performances"

                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "5")
                        .param("showTime", performance1DateTime.toString())
                        .param("minPrice", (performance1Price - 1000).toString())
                        .param("maxPrice", (performance1Price + 5000).toString())
                        .param("region", region.uid)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()

                then("모든 조건을 만족하는 공연 정보만이 조회된다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(listOf(performance1))

                    actual.cursor shouldBe null
                    actual.data shouldContainExactly expected

                }
            }
        }

        given("다수의 공연 정보가 존재할 때") {

            val performances = PerformanceTestDataGenerator.createPerformanceGroupbyRegion(
                performanceCount = 10
            )

            savePerformance(performances)


            `when`("특정 커서를 입력하여 공연 정보를 검색할 시") {
                val uri = "/api/performances"

                // performance004, 003, 002를 조회, 001을 반환해야함.
                val mvcResult = mockMvc.perform(
                    MockMvcRequestBuilders.get(uri)
                        .param("limit", "3")
                        .param("cursor", performances[3].uid)
                )
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn()
                then("특정 커서 이상의 공연 정보를 조회한다.") {
                    val actual = objectMapper.readValue<CursoredResponse<PerformanceSummarySearchResult>>(
                        mvcResult.response.contentAsString,
                        objectMapper.typeFactory.constructParametricType(
                            CursoredResponse::class.java,
                            PerformanceSummarySearchResult::class.java
                        )
                    )

                    val expected = createSearchExpectedOrderByIdDesc(performances)

                    actual.cursor!! shouldBeEqual expected[9].uid
                    actual.data shouldContainExactly expected.subList(6, 9)

                }
            }
        }

        given("공연 정보가 존재할 때 - 상세 조회 검색") {
            val user = UserTestDataGenerator.createUser()


            val placeSeatCnt = 10


            val place = PerformanceTestDataGenerator.createPerformancePlace(
                numSeats = placeSeatCnt
            )

            val showTimes = listOf(
                ZonedDateTime.of(LocalDateTime.of(2024, 1, 1, 10, 0, 0), ZoneId.of("Asia/Seoul")),
            )

            val performance = PerformanceTestDataGenerator.createPerformance(
                place = place,
                showTimes = showTimes
            )

            savePerformance(listOf(performance))
            userRepository.save(user)

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
                            total = placeSeatCnt.toLong(),
                            remaining = placeSeatCnt.toLong() - carts.size
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

            val user = UserTestDataGenerator.createUser()

            val performance = PerformanceTestDataGenerator.createPerformance(
                place = place,
                numShowtimes = 2,
                showTimeStartDateTime = ZonedDateTime.now().plusDays(1)
            )
            val performanceDate = performance.performanceDateTime[0]


            val order = OrderTestDataGenerator.createOrder(
                user = user
            )

            val reservations = OrderTestDataGenerator.createReservations(
                performanceDate,
                performance.performancePlace.seats.subList(0, 3),
                order
            )
            val carts = createCarts(user, performanceDate, performance.performancePlace.seats.subList(3, 4))


            userRepository.save(user)
            savePerformance(listOf(performance))
            saveOrder(order)
            cartRepository.saveAll(carts)

            val jwt = createJwt(user)

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

            val user = UserTestDataGenerator.createUser()
            userRepository.save(user)
            val jwt = createJwt(user)

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
            val performance = PerformanceTestDataGenerator.createPerformance(numShowtimes = 1)
            savePerformance(listOf(performance))

            val user = UserTestDataGenerator.createUser()
            userRepository.save(user)
            val jwt = createJwt(user)

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

            val performances = PerformanceTestDataGenerator.createPerformancesDatesIn(
                dateIn = listOf(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.of("Asia/Seoul"))),
                numShowtimes = 1
            )
            val performance = performances[0]
            val performanceDateTime = performance.performanceDateTime[0]
            savePerformance(performances)

            val user = UserTestDataGenerator.createUser()
            userRepository.save(user)
            val jwt = createJwt(user)

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

            savePerformance(performances)
            `when`("사용자가 5개의 공연 정보를 조회할 시") {
                val uri = "/api/v2/performances"
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
    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)

        PerformanceTestDataGenerator.reset()
        withContext(Dispatchers.IO) {
            cartRepository.deleteAll()
            reservationRepository.deleteAll()
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
}