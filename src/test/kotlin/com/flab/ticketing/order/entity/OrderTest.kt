package com.flab.ticketing.order.entity

import com.flab.ticketing.testutils.PerformanceTestDataGenerator
import com.flab.ticketing.testutils.UnitTest
import com.flab.ticketing.testutils.UserTestDataGenerator
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

class OrderTest : UnitTest() {

    init {
        "Order에 Reservation이 아직 설정되지 않은 경우 빈 문자열을 이름으로 설정한다." {
            val user = UserTestDataGenerator.createUser()

            val order = Order(
                "order001",
                user,
                payment = Order.Payment(1000, "토스 페이", "paymentkey")
            )

            order.name shouldBe ""
        }

        "Order에 Reservation을 추가할 시 주문 이름을 생성 및 조회할 수 있다." {
            val user = UserTestDataGenerator.createUser()
            val performance = PerformanceTestDataGenerator.createPerformance()
            val performanceDateTime = performance.performanceDateTime[0]
            val seats = performance.performancePlace.seats

            val order = Order(
                "order001",
                user,
                payment = Order.Payment(1000, "토스 페이", "paymentkey")
            )

            order.addReservation(performanceDateTime, seats[0])

            order.name shouldBeEqual performance.name + " 좌석 1건"
        }

        "Order에 여러개의 Reservation이 존재할 시 적절한 이름을 설정할 수 있다." {
            val user = UserTestDataGenerator.createUser()
            val performance1 = PerformanceTestDataGenerator.createPerformance(
                name = "공연1"
            )
            val performance2 = PerformanceTestDataGenerator.createPerformance(
                name = "공연2"
            )


            val order = Order(
                "order001",
                user,
                payment = Order.Payment(1000, "토스 페이", "paymentkey")
            )

            order.addReservation(performance1.performanceDateTime[0], performance1.performancePlace.seats[0])
            order.addReservation(performance2.performanceDateTime[0], performance2.performancePlace.seats[0])

            listOf(
                "공연1 좌석 외 1건",
                "공연2 좌석 외 1건"
            ) shouldContainAnyOf listOf(order.name)

        }
    }

}