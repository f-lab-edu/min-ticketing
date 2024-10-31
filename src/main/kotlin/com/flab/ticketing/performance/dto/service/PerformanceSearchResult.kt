package com.flab.ticketing.performance.dto.service

import com.flab.ticketing.performance.entity.PerformanceSearchSchema

data class PerformanceSearchResult(
    val cursor: List<Any>,
    val data: List<PerformanceSearchSchema>
)