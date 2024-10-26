package com.flab.ticketing.performance.repository.dsl

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.entity.Performance

interface CustomPerformanceRepository {

    fun search(
        cursorInfoDto: CursorInfoDto
    ): List<Performance>
}