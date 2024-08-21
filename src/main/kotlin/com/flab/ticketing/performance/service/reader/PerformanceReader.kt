package com.flab.ticketing.performance.service.reader

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.performance.dto.service.PerformanceDetailSearchResult
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.performance.repository.PerformanceDateRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class PerformanceReader(
    private val performanceRepository: PerformanceRepository,
    private val performanceDateRepository: PerformanceDateRepository
) {

    fun searchPerformanceSummaryDto(
        searchConditions: PerformanceSearchConditions,
        cursorInfoDto: CursorInfoDto
    ): List<PerformanceSummarySearchResult> {
        return performanceRepository.search(searchConditions, cursorInfoDto).filterNotNull()
    }

    fun findPerformanceDetailDto(performanceUid: String): PerformanceDetailSearchResult {
        return performanceRepository.findByUid(performanceUid)
            ?: throw NotFoundException(PerformanceErrorInfos.PERFORMANCE_NOT_FOUND)
    }

    fun findPerformanceEntityByUidJoinWithPlace(performanceUid: String): Performance {
        return performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performanceUid)
            ?: throw NotFoundException(
                PerformanceErrorInfos.PERFORMANCE_NOT_FOUND
            )
    }

    fun findDateSummaryDto(performanceUid: String): List<PerformanceDateSummaryResult> {
        return performanceRepository.getDateInfo(performanceUid)
    }

    fun findDateEntityByUid(dateUid: String): PerformanceDateTime {
        return performanceDateRepository.findByUid(dateUid) ?: throw NotFoundException(
            PerformanceErrorInfos.PERFORMANCE_DATE_NOT_FOUND
        )
    }

}