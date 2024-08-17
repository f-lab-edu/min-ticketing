package com.flab.ticketing.performance.service

import com.flab.ticketing.common.dto.CursorInfo
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.performance.dto.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.PerformanceSearchResult
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

    fun search(cursorInfo: CursorInfo, searchConditions: PerformanceSearchConditions): List<PerformanceSearchResult> {
        return performanceRepository.search(searchConditions, cursorInfo).filterNotNull()
    }

    fun searchDetail(uid : String) : PerformanceDetailResponse{
        val performance = performanceRepository.findByUid(uid) ?:
            throw NotFoundException(PerformanceErrorInfos.PERFORMANCE_NOT_FOUND)

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
}