package com.flab.ticketing.order.repository

import com.flab.ticketing.common.OrderTestDataGenerator
import com.flab.ticketing.common.PerformanceTestDataGenerator
import com.flab.ticketing.common.RepositoryTest
import com.flab.ticketing.common.UserTestDataGenerator
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.repository.PerformancePlaceRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.RegionRepository
import com.flab.ticketing.user.repository.UserRepository
import io.kotest.matchers.collections.shouldContainExactly
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

class OrderRepositoryTest :RepositoryTest(){

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
        "커서가 존재하지 않을 때 사용자 별로 주문 최신순으로 정렬해 조회할 수 있다."{
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
            orders.forEachIndexed{idx, order->
                order.addReservation(performance.performanceDateTime[0], performance.performancePlace.seats[idx])
                orderRepository.save(order)
                Thread.sleep(1000)
            }

            val actual = orderRepository.findByUser(user.uid, PageRequest.of(0,10))

            val expectedUidList = orders.sortedBy { it.createdAt }.reversed().map { it.uid }

            actual.map { it.uid } shouldContainExactly expectedUidList
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