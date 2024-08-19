package com.flab.ticketing.performance.service

import com.flab.ticketing.order.repository.ReservationRepository
import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.performance.repository.PerformanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class PerformanceDateReader(
    private val performanceRepository: PerformanceRepository,
    private val reservationRepository: ReservationRepository
) {


    fun getDateInfo(performanceUid: String): List<PerformanceDateSummaryResult> {
        return performanceRepository.getDateInfo(performanceUid)
    }

    fun getReservatedSeatUids(performancePlaceId: Long, dateUid: String): List<String> {
        return reservationRepository.findReservatedSeatUids(performancePlaceId, dateUid)
    }
}