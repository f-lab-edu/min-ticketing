package com.flab.ticketing.order.service

import com.flab.ticketing.common.IntegrationTest
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.UserTestDataGenerator
import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.repository.CartRepository
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import com.flab.ticketing.user.repository.UserRepository
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.get
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations

class ReservationServiceLockTest : IntegrationTest() {

    @Autowired
    private lateinit var reservationService: ReservationService

    @MockkBean
    private lateinit var cartRepository: CartRepository

    @SpykBean
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Autowired
    private lateinit var regionRepository: RegionRepository

    @Autowired
    private lateinit var placeRepository: PerformancePlaceRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    init {

        given("특정 공연 정보가 존재할 때") {
            val performance = PerformanceTestDataGenerator.createPerformance()
            val user = UserTestDataGenerator.createUser()
            val performanceDateTime = performance.performanceDateTime[0]
            val seat = performance.performancePlace.seats[0]

            every { cartRepository.save(any()) } returns Cart("uid", seat, performanceDateTime, user)
            userRepository.save(user)
            savePerformance(listOf(performance))


            // warm-up
            reservationService.reserve(user.uid, performance.uid, performanceDateTime.uid, seat.uid)


            `when`("사용자가 이미 예약된 좌석에 대해 Redis에 Lock 정보가 남아있다면") {
                val user2 = UserTestDataGenerator.createUser(uid = "uid", email = "e@e.com")

                userRepository.save(user2)
                then("DB에 SAVE 요청을 보내지 않고 DuplicatedException을 throw한다.") {
                    shouldThrow<DuplicatedException> {
                        reservationService.reserve(
                            user2.uid,
                            performance.uid,
                            performanceDateTime.uid,
                            seat.uid
                        )
                    }

                    verify(exactly = 1) { cartRepository.save(any()) }
                }
            }

        }

        given("특정 공연 정보가 존재할 때 - Caching 추가 확인"){
            val performance = PerformanceTestDataGenerator.createPerformance()
            val user = UserTestDataGenerator.createUser()
            val performanceDateTime = performance.performanceDateTime[0]
            val seat = performance.performancePlace.seats[0]
            val mockRedisOperation = mockk<ValueOperations<String, String>>()


            every { redisTemplate.opsForValue() } returns mockRedisOperation
            every { mockRedisOperation.setIfAbsent(any(), any(), any()) } returns true
            every { cartRepository.save(any()) } returns Cart("uid", seat, performanceDateTime, user)
            userRepository.save(user)
            savePerformance(listOf(performance))

            `when`("사용자가 이미 예약된 좌석에 대해 Redis에 Lock 정보가 남아있다면") {
                reservationService.reserve(user.uid, performance.uid, performanceDateTime.uid, seat.uid)

                then("Cache를 추가해 저장한다."){
                    val key = "lock:${seat.uid}_${performanceDateTime.uid}"
                    val value = user.uid
                    verify(exactly = 1) { mockRedisOperation.setIfAbsent(key, value, any()) }
                }
            }

        }
    }


    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)
        withContext(Dispatchers.IO) {
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

}