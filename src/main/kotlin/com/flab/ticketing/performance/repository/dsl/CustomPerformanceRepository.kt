package com.flab.ticketing.performance.repository.dsl

import com.flab.ticketing.common.dto.CursorInfo
import com.flab.ticketing.performance.dto.PerformanceDetailSearchResult
import com.flab.ticketing.performance.dto.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.PerformanceSearchResult

interface CustomPerformanceRepository {
    fun search(searchConditions: PerformanceSearchConditions, cursorInfo: CursorInfo): List<PerformanceSearchResult?>
}