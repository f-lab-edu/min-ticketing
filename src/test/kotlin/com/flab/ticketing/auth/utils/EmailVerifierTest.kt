package com.flab.ticketing.auth.utils

import com.flab.ticketing.auth.entity.EmailVerifyInfo
import com.flab.ticketing.auth.exception.AuthErrorInfos.EMAIL_NOT_VERIFIED
import com.flab.ticketing.auth.exception.AuthErrorInfos.EMAIL_VERIFY_INFO_NOT_FOUND
import com.flab.ticketing.auth.repository.EmailVerifyInfoRepository
import com.flab.ticketing.common.exception.BusinessIllegalStateException
import com.flab.ticketing.testutils.UnitTest
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import java.util.*

class EmailVerifierTest : UnitTest() {
    private val emailVerifyInfoRepository: EmailVerifyInfoRepository = mockk()
    private val emailVerifier: EmailVerifier = EmailVerifier(emailVerifyInfoRepository)

    init {
        "이메일 인증이 완료된 정보가 DB에 저장되어 있다면 검증 확인 시 오류를 throw하지 않는다." {
            // given
            val email = "email@email.com"
            val code = "1234AB"
            val emailVerifyInfo = EmailVerifyInfo(email, code, true)

            every { emailVerifyInfoRepository.findById(email) } returns Optional.of(emailVerifyInfo)

            // when & then
            shouldNotThrow<Exception> { emailVerifier.checkVerified(email) }
        }

        "인증 정보가 저장되지 않은 이메일을 검증 시도할 시 BusinessIllegalStateException에 적절한 ErrorInfo를 담아 throw한다." {
            // given
            val email = "notSaved@email.com"

            every { emailVerifyInfoRepository.findById(email) } returns Optional.empty()

            // when & then
            val e = shouldThrow<BusinessIllegalStateException> {
                emailVerifier.checkVerified(email)
            }

            e.info shouldBeEqual EMAIL_VERIFY_INFO_NOT_FOUND

        }
        "인증 정보가 저장되어 있으나 인증이 완료되지 않은 사용자가 검증 시도시 BusinessIllegalStateException에 적절한 ErrorInfo를 담아 throw한다." {

            // given
            val email = "notVerified@email.com"

            every { emailVerifyInfoRepository.findById(email) } returns Optional.of(EmailVerifyInfo(email, "1234ab"))


            // when & then
            val e = shouldThrow<BusinessIllegalStateException> {
                emailVerifier.checkVerified(email)
            }

            e.info shouldBeEqual EMAIL_NOT_VERIFIED
        }

    }


}