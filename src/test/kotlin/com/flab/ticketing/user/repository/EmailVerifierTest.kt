package com.flab.ticketing.user.repository

import com.flab.ticketing.user.entity.EmailVerifyInfo
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import java.util.*

class EmailVerifierTest : BehaviorSpec() {
    private val emailVerifyInfoRepository: EmailVerifyInfoRepository = mockk()
    private val emailVerifier: EmailVerifier = EmailVerifier(emailVerifyInfoRepository)


    init {
        given("이메일 인증이 완료된 사용자가 존재할 때") {
            val email = "email@email.com"
            val code = "1234AB"

            val emailVerifyInfo = EmailVerifyInfo(email, code, true)

            every { emailVerifyInfoRepository.findById(email) } returns Optional.of(emailVerifyInfo)


            `when`("해당 사용자가 이메일 검증이 되었는지 확인한다면") {
                then("오류를 throw하지 않는다.") {
                    shouldNotThrow<Exception> { emailVerifier.checkVerified(email) }
                }
            }
        }

    }


}
