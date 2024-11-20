package com.flab.ticketing.order.entity

import com.flab.ticketing.testutils.UnitTest
import com.flab.ticketing.testutils.fixture.PerformanceFixture
import com.flab.ticketing.testutils.fixture.UserFixture
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

class OrderTest : UnitTest() {

    init {
        "Order에 Reservation이 아직 설정되지 않은 경우 빈 문자열을 이름으로 설정한다." {
            // given
            val user = UserFixture.createUser()

            // when
            val order = Order(
                "order001",
                user,
                payment = Order.Payment(1000, "토스 페이", "paymentkey")
            )

            // then
            order.name shouldBe ""
        }

        "Order에 Reservation을 추가할 시 주문 이름을 생성 및 조회할 수 있다." {
            // given
            val user = UserFixture.createUser()
            val performance = PerformanceFixture.createPerformance()
            val performanceDateTime = performance.performanceDateTime[0]
            val seats = performance.performancePlace.seats

            // when
            val order = Order(
                "order001",
                user,
                payment = Order.Payment(1000, "토스 페이", "paymentkey")
            )

            order.addReservation(performanceDateTime, seats[0])

            // then
            order.name shouldBeEqual performance.name + " 좌석 1건"
        }

        "Order에 여러개의 Reservation이 존재할 시 적절한 이름을 설정할 수 있다." {
            // given
            val user = UserFixture.createUser()
            val performance1 = PerformanceFixture.createPerformance(
                name = "공연1"
            )
            val performance2 = PerformanceFixture.createPerformance(
                name = "공연2"
            )

            // when
            val order = Order(
                "order001",
                user,
                payment = Order.Payment(1000, "토스 페이", "paymentkey")
            )

            order.addReservation(performance1.performanceDateTime[0], performance1.performancePlace.seats[0])
            order.addReservation(performance2.performanceDateTime[0], performance2.performancePlace.seats[0])

            // then
            listOf(
                "공연1 좌석 외 1건",
                "공연2 좌석 외 1건"
            ) shouldContainAnyOf listOf(order.name)

        }
    }

}