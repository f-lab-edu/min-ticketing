package com.flab.ticketing.performance.service

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.order.service.reader.ReservationReader
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.response.PerformanceDateDetailResponse
import com.flab.ticketing.performance.dto.response.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.performance.service.reader.PerformanceReader
import com.flab.ticketing.performance.service.verifier.PerformanceVerifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class PerformanceService(
    private val performanceVerifier: PerformanceVerifier,
    private val performanceReader: PerformanceReader,
    private val reservationReader: ReservationReader
) {

    fun search(
        cursorInfoDto: CursorInfoDto,
        searchConditions: PerformanceSearchConditions
    ): List<PerformanceSummarySearchResult> {
        return performanceReader.searchPerformanceSummaryDto(searchConditions, cursorInfoDto)
    }

    fun searchDetail(uid: String): PerformanceDetailResponse {
        val performance = performanceReader.findPerformanceDetailDto(uid)
        val dateSummaryDtoList = performanceReader.findDateSummaryDto(uid)
        val dateInfo = convertDateDtoToDateInfo(dateSummaryDtoList)

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
        val performance = performanceReader.findPerformanceEntityByUidJoinWithPlace(performanceUid)
        val performanceDateTime = performanceReader.findDateEntityByUid(performanceUid, performanceDateUid)

        performanceDateTime.checkPassed()

        val reservedSeatUidList =
            reservationReader.findReserveUidInPlace(performance.performancePlace.id, performanceDateUid)

        val seatTable = createSeatTable(
            performanceSeats = performance.performancePlace.seats,
            reservedSeatUidList = reservedSeatUidList
        )

        return PerformanceDateDetailResponse(
            performanceDateUid,
            performance.price,
            seatTable
        )
    }


    private fun createSeatTable(
        performanceSeats: List<PerformancePlaceSeat>,
        reservedSeatUidList: List<String>
    ): List<List<PerformanceDateDetailResponse.SeatInfo>> {
        val orderedDateSeatInfo = mutableListOf<MutableList<PerformanceDateDetailResponse.SeatInfo>>()

        performanceSeats.sortedWith(
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
                        reservedSeatUidList.contains(it.uid)
                    )
                )
            }
        return orderedDateSeatInfo
    }

    private fun convertDateDtoToDateInfo(dtoList: List<PerformanceDateSummaryResult>): List<PerformanceDetailResponse.DateInfo> {
        return dtoList.map {
            PerformanceDetailResponse.DateInfo(
                it.uid,
                it.showTime.toLocalDateTime(),
                it.totalSeats,
                it.totalSeats - it.reservedSeats
            )
        }
    }
}