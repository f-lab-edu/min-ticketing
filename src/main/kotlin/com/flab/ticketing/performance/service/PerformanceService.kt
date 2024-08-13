package com.flab.ticketing.performance.service

import com.flab.ticketing.common.dto.CursorInfo
import com.flab.ticketing.performance.dto.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.PerformanceSearchResult
import com.flab.ticketing.performance.repository.PerformanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class PerformanceService(
    private val performanceRepository: PerformanceRepository
) {

    fun search(cursorInfo: CursorInfo, searchConditions: PerformanceSearchConditions): List<PerformanceSearchResult> {
        return performanceRepository.search(searchConditions, cursorInfo).filterNotNull()
    }

}