package com.flab.ticketing.common.exception

enum class CommonErrorInfos(override val code: String, override val message: String) : ErrorInfo {

    INVALID_FIELD("COMMON-001", " 필드의 값이 올바르지 않습니다."),
    SERVICE_ERROR("COMMON-002", "서버 처리 중 알 수 없는 오류가 발생했습니다."),
    INVALID_METHOD("COMMON-003", "HTTP 메소드를 올바르게 설정하지 않았습니다."),
    EXTERNAL_API_ERROR("COMMON-004", "외부 API 오류가 발생하였습니다.")
}