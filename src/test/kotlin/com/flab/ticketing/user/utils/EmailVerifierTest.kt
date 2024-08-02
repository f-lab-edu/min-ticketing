package com.flab.ticketing.user.utils

import com.flab.ticketing.common.exception.ForbiddenException
import com.flab.ticketing.user.entity.EmailVerifyInfo
import com.flab.ticketing.user.exception.UserErrorInfos.EMAIL_NOT_VERIFIED
import com.flab.ticketing.user.exception.UserErrorInfos.EMAIL_VERIFY_INFO_NOT_FOUND
import com.flab.ticketing.user.repository.EmailVerifyInfoRepository
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
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
        given("이메일 인증 정보가 저장되지 않았을 때") {
            val email = "notSaved@email.com"

            every { emailVerifyInfoRepository.findById(email) } returns Optional.empty()

            `when`("저장되지 않은 사용자가 이메일 검증을 요청했을 시") {
                then("Forbidden 오류와 이에 맞는 ErrorInfo를 throw한다.") {
                    val e = shouldThrow<ForbiddenException> {
                        emailVerifier.checkVerified(email)
                    }

                    e.info.code shouldBeEqual EMAIL_VERIFY_INFO_NOT_FOUND.code
                    e.info.message shouldBeEqual EMAIL_VERIFY_INFO_NOT_FOUND.message
                }
            }
        }
        given("이메일 인증이 완료되지 않았을 때") {
            val email = "notVerified@email.com"

            every { emailVerifyInfoRepository.findById(email) } returns Optional.of(EmailVerifyInfo(email, "1234ab"))

            `when`("해당 사용자가 이메일 검증을 시도할 때") {
                then("ForbiddenException과 적절한 ErrorInfo를 throw한다") {
                    val e = shouldThrow<ForbiddenException> {
                        emailVerifier.checkVerified(email)
                    }

                    e.info shouldBeEqual EMAIL_NOT_VERIFIED
                }
            }
        }
    }


}
