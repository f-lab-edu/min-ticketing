package com.flab.ticketing.performance.service

import com.flab.ticketing.common.dto.CursorInfo
import com.flab.ticketing.order.service.ReservationService
import com.flab.ticketing.performance.dto.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.PerformanceSearchResult
import com.flab.ticketing.performance.repository.PerformanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class PerformanceService(
    private val performanceRepository: PerformanceRepository,
    private val reservationService: ReservationService
) {

    fun search(cursorInfo: CursorInfo, searchConditions: PerformanceSearchConditions): List<PerformanceSearchResult> {
        return performanceRepository.search(searchConditions, cursorInfo).filterNotNull()
    }

    fun searchDetail(uid : String) : PerformanceDetailResponse{
        val performance = performanceRepository.findByUid(uid)
        if(performance == null){
            throw Exception()
        }


        val seatSize = performance.performancePlace.seats.size

        val dateInfo = performance.performanceDateTime.map {
            PerformanceDetailResponse.DateInfo(
                it.uid,
                it.showTime.toLocalDateTime(),
                seatSize.toLong(),
                seatSize - reservationService.getReservationCount(it.uid),
            ) }


        return PerformanceDetailResponse(
            performance.uid,
            performance.image,
            performance.name,
            performance.performancePlace.region.name,
            performance.performancePlace.name,
            performance.price,
            performance.description,
            dateInfo
        )
    }
}