package com.flab.ticketing.performance.exception

import com.flab.ticketing.common.exception.ErrorInfo

enum class PerformanceErrorInfos(override val code: String, override val message: String) : ErrorInfo {

    PERFORMANCE_NOT_FOUND("PERFORMANCE-001", "공연 정보를 조회할 수 없습니다."),
    INVALID_PERFORMANCE_DATE("PERFORMANCE-002", "공연 날짜의 정보가 올바르지 않습니다."),
    PERFORMANCE_ALREADY_PASSED("PERFORMANCE-003", "이미 종료된 공연입니다."),
    PERFORMANCE_SEAT_INFO_INVALID("PERFORMANCE-004", "좌석 정보가 올바르지 않습니다.")
}