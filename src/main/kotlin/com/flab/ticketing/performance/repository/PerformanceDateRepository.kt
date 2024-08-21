package com.flab.ticketing.performance.repository

import com.flab.ticketing.performance.entity.PerformanceDateTime
import org.springframework.data.repository.Repository


@org.springframework.stereotype.Repository
interface PerformanceDateRepository : Repository<PerformanceDateTime, Long> {
    fun findByUid(uid: String): PerformanceDateTime?
}