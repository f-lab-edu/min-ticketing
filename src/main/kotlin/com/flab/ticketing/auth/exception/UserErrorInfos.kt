package com.flab.ticketing.auth.exception

import com.flab.ticketing.common.exception.ErrorInfo

enum class UserErrorInfos(override val code: String, override val message: String) : ErrorInfo {
    DUPLICATED_EMAIL("USER-001", "이미 가입한 이메일입니다."),
    EMAIL_VERIFY_INFO_NOT_FOUND("USER-003", "이메일 인증 정보를 조회할 수 없습니다."),
    EMAIL_VERIFYCODE_INVALID("USER-004", "이메일 인증 코드가 잘못되었습니다."),
    EMAIL_NOT_VERIFIED("USER-005", "이메일 인증처리가 완료되지 않았습니다."),
    PASSWORD_CONFIRM_NOT_EQUALS("USER-006", "비밀번호와 비밀번호 확인 값이 동일하지 않습니다."),
    LOGIN_INFO_INVALID("USER-007", "로그인 정보의 형태가 올바르지 않습니다."),
    LOGIN_FAILED("USER-008", "이메일 혹은 비밀번호가 잘못되었습니다."),
    AUTH_INFO_INVALID("USER-009", "인증 정보가 없거나 올바르게 전달되지 않았습니다."),
    AUTH_INFO_EXPIRED("USER-010", "인증 정보가 만료되었습니다.")

}