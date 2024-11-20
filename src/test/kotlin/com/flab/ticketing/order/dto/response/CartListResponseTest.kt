package com.flab.ticketing.order.dto.response

import com.flab.ticketing.testutils.PerformanceTestDataGenerator
import com.flab.ticketing.testutils.UnitTest
import com.flab.ticketing.testutils.UserTestDataGenerator
import com.flab.ticketing.order.entity.Cart
import io.kotest.matchers.collections.shouldContainAll

class CartListResponseTest : UnitTest() {

    init {
        "Cart 리스트를 입력받아 CartListResponse 객체를 생성할 수 있다." {
            val user = UserTestDataGenerator.createUser()

            val performance = PerformanceTestDataGenerator.createPerformance()
            val performanceDateTime = performance.performanceDateTime[0]

            val carts = listOf(
                Cart(
                    uid = "cart001",
                    user = user,
                    seat = performance.performancePlace.seats[0],
                    performanceDateTime = performanceDateTime
                ),

                Cart(
                    uid = "cart002",
                    user = user,
                    seat = performance.performancePlace.seats[1],
                    performanceDateTime = performanceDateTime
                )
            )

            val actual = CartListResponse.of(carts)
            val expected = listOf(
                CartListResponse.CartInfo(
                    "cart001",
                    performanceDateTime.showTime,
                    performance.name,
                    performance.price,
                    performance.performancePlace.seats[0].name
                ),
                CartListResponse.CartInfo(
                    "cart002",
                    performanceDateTime.showTime,
                    performance.name,
                    performance.price,
                    performance.performancePlace.seats[1].name
                ),
            )


            actual.data shouldContainAll expected
        }
    }

}