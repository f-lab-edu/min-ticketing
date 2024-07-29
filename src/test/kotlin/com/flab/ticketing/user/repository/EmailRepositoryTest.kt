package com.flab.ticketing.user.repository

import io.kotest.assertions.fail
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration
import java.time.temporal.ChronoUnit


class EmailRepositoryTest : BehaviorSpec() {

    val redisTemplate: RedisTemplate<String, String> = mockk()

    val emailRepository = EmailRepository(redisTemplate)

    init {
        given("이메일과 코드가 주어졌을 때") {
            val givenEmail = "email@Email.com"
            val givenCode = "code12"

            every {
                redisTemplate.opsForValue().set(givenEmail, givenCode, Duration.of(1, ChronoUnit.HOURS))
            } returns Unit

            `when`("이메일 코드를 저장하면") {

                emailRepository.saveCode(givenEmail, givenCode)

                then("Redis에 한시간 동안 저장된다.") {
                    verify { redisTemplate.opsForValue().set(givenEmail, givenCode, Duration.of(1, ChronoUnit.HOURS)) }

                }
            }

        }

        given("이메일이 주어져 있고, 이메일 인증 코드가 저장되어 있을 때") {
            val email = "email@Email.com"
            val expectedCode = "code12"
            every { redisTemplate.opsForValue().get(email) } returns expectedCode

            `when`("인증코드를 조회하면") {
                val actualCode = emailRepository.getCode(email)

                then("저장된 이메일을 반환받을 수 있다.") {
                    if (actualCode == null) {
                        fail("인증 코드는 저장된 결과가 나와야한다.")
                    }
                    actualCode shouldBeEqual expectedCode
                }
            }
        }
    }

}
