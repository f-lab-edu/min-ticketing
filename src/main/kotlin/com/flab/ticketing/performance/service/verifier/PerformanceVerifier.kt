package com.flab.ticketing.performance.service.verifier

import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.performance.entity.PerformancePlace
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import org.springframework.stereotype.Component


@Component
class PerformanceVerifier {

    fun checkIsSeatInPlace(place: PerformancePlace, seatUid: String) {
        if (!place.seats.map { it.uid }.contains(seatUid)) {
            throw InvalidValueException(PerformanceErrorInfos.PERFORMANCE_SEAT_INFO_INVALID)
        }
    }
}