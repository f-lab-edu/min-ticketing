package com.flab.ticketing.performance.repository.dsl

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.entity.Performance

interface CustomPerformanceRepository {
    fun search(
        searchConditions: PerformanceSearchConditions,
        cursorInfoDto: CursorInfoDto
    ): List<PerformanceSummarySearchResult?>


    fun search(
        cursorInfoDto: CursorInfoDto
    ): List<Performance>
}