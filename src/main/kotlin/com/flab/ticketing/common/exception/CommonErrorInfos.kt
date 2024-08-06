package com.flab.ticketing.common.exception

enum class CommonErrorInfos(override val code: String, override val message: String) : ErrorInfo {

    INVALID_FIELD("COMMON-001", " 필드의 값이 올바르지 않습니다."),
    SERVICE_ERROR("COMMON-002", "서버 처리 중 알 수 없는 오류가 발생했습니다.")

}