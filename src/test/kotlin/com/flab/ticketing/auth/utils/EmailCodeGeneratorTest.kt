package com.flab.ticketing.auth.utils

import com.flab.ticketing.testutils.UnitTest
import io.kotest.matchers.string.shouldMatch

class EmailCodeGeneratorTest : UnitTest() {
    val emailCodeGenerator: EmailCodeGenerator = EmailCodeGenerator()

    init {
        "랜덤한 6자리의 영문(대문자)과 숫자를 포함한 문자열을 생성할 수 있다." {
            emailCodeGenerator.createEmailCode() shouldMatch "^[A-Z0-9]{6}\$"
        }
    }


}