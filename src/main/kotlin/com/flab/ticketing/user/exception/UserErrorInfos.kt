package com.flab.ticketing.user.exception

import com.flab.ticketing.common.exception.ErrorInfo

enum class UserErrorInfos(override val code : String, override val message : String) : ErrorInfo{
    DUPLICATED_EMAIL("USER-001", "이미 가입한 이메일입니다."),
    EMAIL_EXPRESSION_INVALID("USER-002", "이메일 형식이 올바르지 않습니다."),
    EMAIL_VERIFYCODE_NOT_FOUND("USER-003", "이메일 인증 코드를 조회할 수 없습니다."),
    EMAIL_VERIFYCODE_INVALID("USER-004", "이메일 인증 코드가 잘못되었습니다.")
}