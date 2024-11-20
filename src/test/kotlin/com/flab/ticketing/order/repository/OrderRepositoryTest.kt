package com.flab.ticketing.order.repository

import com.flab.ticketing.testutils.generator.OrderTestDataGenerator
import com.flab.ticketing.testutils.generator.PerformanceTestDataGenerator
import com.flab.ticketing.testutils.RepositoryTest
import com.flab.ticketing.testutils.generator.UserTestDataGenerator
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.exception.BadRequestException
import com.flab.ticketing.order.dto.request.OrderSearchConditions
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import com.flab.ticketing.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired

class OrderRepositoryTest : RepositoryTest() {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var regionRepository: RegionRepository

    @Autowired
    private lateinit var placeRepository: PerformancePlaceRepository

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    init {
        "커서가 존재하지 않을 때 사용자 별로 주문 최신순으로 정렬해 조회할 수 있다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )

            val orders = List(5) {
                OrderTestDataGenerator.createOrder(
                    uid = "order-00$it",
                    user = user
                )
            }

            userRepository.save(user)
            savePerformance(listOf(performance))
            orders.forEachIndexed { idx, order ->
                order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[idx])
                orderRepository.save(order)
                Thread.sleep(100)
            }

            val actual = orderRepository.findByUser(user.uid, CursorInfoDto(), OrderSearchConditions())

            val expectedUidList = orders.sortedByDescending { it.createdAt }.map { it.uid }

            actual.map { it.uid } shouldContainExactly expectedUidList
        }

        "커서가 존재할 때 사용자 별로 주문 최신순으로 정렬해 조회할 수 있다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )

            val orders = List(5) {
                OrderTestDataGenerator.createOrder(
                    uid = "order-00$it",
                    user = user
                )
            }

            userRepository.save(user)
            savePerformance(listOf(performance))
            orders.forEachIndexed { idx, order ->
                order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[idx])
                orderRepository.save(order)
            }

            val sortedOrders = orders.sortedByDescending { it.id }

            val actual = orderRepository.findByUser(
                user.uid,
                CursorInfoDto(cursor = sortedOrders[2].uid),
                OrderSearchConditions()
            )
            val expectedUidList = listOf(sortedOrders[2].uid, sortedOrders[3].uid, sortedOrders[4].uid)

            actual.map { it.uid } shouldContainExactly expectedUidList

        }

        "주문을 저장할 때 주문의 Reservation 객체가 0개라면 exception을 throw한다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance()

            val order = Order("order-001", user, Order.Payment(0, "카드", "paymentkey"))

            userRepository.save(user)
            performanceRepository.save(performance)

            val e = shouldThrow<BadRequestException> {
                orderRepository.save(order)
            }

            e.info shouldBe OrderErrorInfos.ORDER_MUST_MINIMUM_ONE_RESERVATION

        }

        "주문을 조회할 때 Status로 조회할 수 있다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance(
                place = PerformanceTestDataGenerator.createPerformancePlace(numSeats = 10)
            )

            val orders = List(5) {
                OrderTestDataGenerator.createOrder(
                    uid = "order-00$it",
                    user = user
                )
            }

            userRepository.save(user)
            savePerformance(listOf(performance))
            orders.forEachIndexed { idx, order ->
                order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[idx])
                orderRepository.save(order)
            }
            val canceledOrders = orders.subList(0, 2)
            canceledOrders.forEach { it.status = Order.OrderStatus.CANCELED }

            val actual = orderRepository.findByUser(
                user.uid,
                CursorInfoDto(),
                OrderSearchConditions(status = Order.OrderStatus.CANCELED)
            )

            actual shouldContainAll canceledOrders

        }
    }


    private fun savePerformance(performances: List<Performance>) {
        regionRepository.save(performances[0].performancePlace.region)
        placeRepository.save(performances[0].performancePlace)

        performances.forEach {
            performanceRepository.save(it)
        }
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        super.afterEach(testCase, result)

        PerformanceTestDataGenerator.reset()
        withContext(Dispatchers.IO) {
            orderRepository.deleteAll()
            userRepository.deleteAll()
            performanceRepository.deleteAll()
            placeRepository.deleteAll()
            regionRepository.deleteAll()
        }
    }
}