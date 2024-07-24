package com.flab.ticketing.user.utils

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldMatch

class EmailCodeGeneratorTest : BehaviorSpec(){
    val emailCodeGenerator : EmailCodeGenerator = EmailCodeGenerator()

    init {
        given("이메일 코드 생성 요청 시"){

            `when`("이메일 생성 코드를 호출하면"){
                val actual = emailCodeGenerator.createEmailCode()

                `then`("랜덤한 6자리의 영문(대문자)과 숫자를 포함한 문자열을 생성할 수 있다."){
                    actual shouldMatch "^[A-Z0-9]{6}\$"
                }
            }
        }
    }


}
