package com.flab.ticketing.order.service

import com.flab.ticketing.order.repository.ReservationRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import org.springframework.stereotype.Service


@Service
class ReservationService(
    private val reservationRepository: ReservationRepository
){

    fun getReservationCount(performanceDateTimeUid : String): Long{
        return reservationRepository.countByPerformanceDateTime(performanceDateTimeUid)
    }

}