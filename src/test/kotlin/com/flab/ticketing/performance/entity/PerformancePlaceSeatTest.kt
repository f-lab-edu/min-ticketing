package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.UnitTest
import io.kotest.matchers.equals.shouldBeEqual

class PerformancePlaceSeatTest : UnitTest() {

    init {
        "Seat 객체를 생성할 때 알맞은 이름을 생성할 수 있다." {

            val dummy = PerformancePlace("name", listOf())
            val row = 1
            val column = 20
            val seat = PerformancePlaceSeat("uid", row, column, dummy)

            seat.name shouldBeEqual "A20"
        }
        "Seat 객체를 생성할 때 Row가 알파벳 범위를 넘어설 경우, AA, AB와 같이 추가 알파벳을 붙여 이름을 생성한다." {
            val dummy = PerformancePlace("name", listOf())
            val row = 28
            val column = 20
            val seat = PerformancePlaceSeat("uid", row, column, dummy)

            seat.name shouldBeEqual "AB20"
        }
    }

}
