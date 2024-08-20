package com.flab.ticketing.performance.service

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.exception.BadRequestException
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.performance.dto.response.PerformanceDateDetailResponse
import com.flab.ticketing.performance.dto.response.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.performance.repository.PerformanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class PerformanceService(
    private val performanceRepository: PerformanceRepository,
    private val performanceDateReader: PerformanceDateReader
) {

    fun search(
        cursorInfoDto: CursorInfoDto,
        searchConditions: PerformanceSearchConditions
    ): List<PerformanceSummarySearchResult> {
        return performanceRepository.search(searchConditions, cursorInfoDto).filterNotNull()
    }

    fun searchDetail(uid: String): PerformanceDetailResponse {
        val performance =
            performanceRepository.findByUid(uid) ?: throw NotFoundException(PerformanceErrorInfos.PERFORMANCE_NOT_FOUND)

        val dateInfo = performanceDateReader.getDateInfo(uid).map {
            PerformanceDetailResponse.DateInfo(
                it.uid,
                it.showTime.toLocalDateTime(),
                it.totalSeats,
                it.totalSeats - it.reservatedSeats
            )
        }

        return PerformanceDetailResponse(
            performance.uid,
            performance.image,
            performance.title,
            performance.regionName,
            performance.placeName,
            performance.price,
            performance.description,
            dateInfo
        )
    }

    fun getPerformanceSeatInfo(performanceUid: String, performanceDateUid: String): PerformanceDateDetailResponse {
        val performance =
            performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performanceUid) ?: throw NotFoundException(
                PerformanceErrorInfos.PERFORMANCE_NOT_FOUND
            )

        if (!performance.performanceDateTime.map { it.uid }.contains(performanceDateUid)) {
            throw BadRequestException(PerformanceErrorInfos.INVALID_PERFORMANCE_DATE)
        }

        val orderedDateSeatInfo = mutableListOf<MutableList<PerformanceDateDetailResponse.SeatInfo>>()

        val dateSeatUids =
            performanceDateReader.getReservatedSeatUids(performance.performancePlace.id, performanceDateUid)

        performance.performancePlace.seats.sortedWith(
            compareBy<PerformancePlaceSeat> { it.rowNum }
                .thenBy { it.columnNum })
            .forEach {
                if (orderedDateSeatInfo.size <= it.rowNum - 1) {
                    orderedDateSeatInfo.add(mutableListOf())
                }
                orderedDateSeatInfo[it.rowNum - 1].add(
                    PerformanceDateDetailResponse.SeatInfo(
                        it.uid,
                        it.name,
                        dateSeatUids.contains(it.uid)
                    )
                )
            }

        return PerformanceDateDetailResponse(
            performanceDateUid,
            performance.price,
            orderedDateSeatInfo
        )
    }

}