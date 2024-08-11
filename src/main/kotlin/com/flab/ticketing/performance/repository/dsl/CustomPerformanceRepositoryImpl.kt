package com.flab.ticketing.performance.repository.dsl

import com.flab.ticketing.common.dto.CursorInfo
import com.flab.ticketing.performance.dto.PerformanceDetailSearchResult
import com.flab.ticketing.performance.dto.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.PerformanceSearchResult
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.stereotype.Repository


@Repository
class CustomPerformanceRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor
) : CustomPerformanceRepository {
    override fun search(
        searchConditions: PerformanceSearchConditions,
        cursorInfo: CursorInfo
    ): List<PerformanceSearchResult> {
        TODO("Not yet implemented")
    }

    override fun searchDetail(uid: String): PerformanceDetailSearchResult {
        TODO("Not yet implemented")
    }
}