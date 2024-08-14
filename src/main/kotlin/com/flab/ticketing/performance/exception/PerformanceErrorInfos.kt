package com.flab.ticketing.performance.exception

import com.flab.ticketing.common.exception.ErrorInfo

enum class PerformanceErrorInfos (override val code: String, override val message: String) : ErrorInfo {

    PERFORMANCE_NOT_FOUND("PERFORMANCE-001", "공연 정보를 조회할 수 없습니다.")
}