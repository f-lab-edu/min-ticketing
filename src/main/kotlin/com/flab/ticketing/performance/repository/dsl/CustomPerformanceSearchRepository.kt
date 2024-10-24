package com.flab.ticketing.performance.repository.dsl

import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.entity.PerformanceSearchSchema

interface CustomPerformanceSearchRepository {

    fun search(
        searchConditions: PerformanceSearchConditions,
        sortValues: List<Any>?,
        limit: Int
    ): Pair<List<Any>, List<PerformanceSearchSchema>>

}