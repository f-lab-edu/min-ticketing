package com.flab.ticketing.common.exception

enum class CommonErrorInfos(override val code: String, override val message: String) : ErrorInfo {

    INVALID_FIELD("COMMON-001", " 필드의 값이 올바르지 않습니다.")

}