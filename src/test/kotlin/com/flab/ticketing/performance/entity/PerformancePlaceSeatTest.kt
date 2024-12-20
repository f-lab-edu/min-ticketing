package com.flab.ticketing.performance.entity

import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.testutils.UnitTest
import com.flab.ticketing.testutils.fixture.PerformanceFixture
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

class PerformancePlaceSeatTest : UnitTest() {

    init {
        "Seat 객체를 생성할 때 알맞은 이름을 생성할 수 있다." {
            // given
            val dummy = PerformanceFixture.createPerformancePlace()
            val row = 1
            val column = 20

            // when
            val seat = PerformancePlaceSeat("uid", row, column, dummy)

            // then
            seat.name shouldBeEqual "A20"
        }
        "Seat 객체를 생성할 때 Row가 알파벳 범위를 넘어설 경우, AA, AB와 같이 추가 알파벳을 붙여 이름을 생성한다." {
            // given
            val dummy = PerformanceFixture.createPerformancePlace()
            val row = 28
            val column = 20
            // when
            val seat = PerformancePlaceSeat("uid", row, column, dummy)

            // then
            seat.name shouldBeEqual "AB20"
        }

        "Seat uid를 인자로 입력받아 해당 seat가 place에 속한 seat가 아닐 시 InvalidValueException을 throw 한다." {
            // given
            val place = PerformanceFixture.createPerformancePlace()
            val invalidSeatUid = "placeseat12032104120401204"

            // when & then
            val e = shouldThrow<InvalidValueException> {
                place.findSeatIn(invalidSeatUid)
            }

            e.info shouldBe PerformanceErrorInfos.PERFORMANCE_SEAT_INFO_INVALID

        }

        "place에 seatUid로 검색할 때 place에 속할 시 아무 Exception을 throw하지 않는다." {
            // given
            val place = PerformanceFixture.createPerformancePlace()
            val seatUid = place.seats[0].uid

            // when & then
            shouldNotThrow<Exception> { place.findSeatIn(seatUid) }
        }
    }


}