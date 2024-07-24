package com.flab.ticketing.user.exception

enum class UserExceptionMessages(val message : String) {
    DUPLICATED_EMAIL("이미 가입한 이메일입니다."),
    EMAIL_EXPRESSION_INVALID("이메일 형식이 올바르지 않습니다."),
    EMAIL_VERIFYCODE_NOT_FOUND("이메일 인증 코드를 조회할 수 없습니다."),
    EMAIL_VERIFYCODE_INVALID("이메일 인증 코드가 잘못되었습니다.")
}