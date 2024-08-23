package com.flab.ticketing.performance.service.verifier

import com.flab.ticketing.common.exception.BadRequestException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlace
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import org.springframework.stereotype.Component
import java.time.ZonedDateTime


@Component
class PerformanceVerifier {
    
    fun checkIsExpired(performanceDateTime: PerformanceDateTime, compareTime: ZonedDateTime = ZonedDateTime.now()) {
        if (performanceDateTime.isExpired(compareTime)) {
            throw BadRequestException(PerformanceErrorInfos.PERFORMANCE_ALREADY_PASSED)
        }
    }

    fun checkIsSeatInPlace(place: PerformancePlace, seatUid: String) {
        if (!place.seats.map { it.uid }.contains(seatUid)) {
            throw InvalidValueException(PerformanceErrorInfos.PERFORMANCE_SEAT_INFO_INVALID)
        }
    }
}