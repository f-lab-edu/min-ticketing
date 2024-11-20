package com.flab.ticketing.performance.entity

import com.flab.ticketing.testutils.generator.PerformanceTestDataGenerator
import com.flab.ticketing.testutils.UnitTest
import com.flab.ticketing.common.exception.BusinessIllegalStateException
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PerformanceDateTimeTest : UnitTest() {

    init {
        "이미 날짜가 지난 공연이면 PerformanceDateTime이 만료되었는지 확인시 BusinessIllegalStateException을 반환한다." {
            val passedPerformanceDateTime = PerformanceTestDataGenerator.createPerformance(
                showTimeStartDateTime = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault())
            ).performanceDateTime[0]

            val e = shouldThrow<BusinessIllegalStateException> {
                passedPerformanceDateTime.checkPassed(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()))
            }

            e.info shouldBe PerformanceErrorInfos.PERFORMANCE_ALREADY_PASSED

        }

        "날짜가 지나지 않은 공연이면 PerformanceDateTime이 만료되었는지 확인시 Exception을 throw 하지 않는다." {
            val passedPerformanceDateTime = PerformanceTestDataGenerator.createPerformance(
                showTimeStartDateTime = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault())
            ).performanceDateTime[0]

            shouldNotThrow<Exception> {
                passedPerformanceDateTime.checkPassed(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()))
            }

        }
    }

}