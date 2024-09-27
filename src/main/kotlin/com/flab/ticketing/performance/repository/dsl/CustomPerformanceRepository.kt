package com.flab.ticketing.performance.repository.dsl

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult

interface CustomPerformanceRepository {
    fun search(
        searchConditions: PerformanceSearchConditions,
        cursorInfoDto: CursorInfoDto
    ): List<PerformanceSummarySearchResult?>
}