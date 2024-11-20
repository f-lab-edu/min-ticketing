package com.flab.ticketing.auth.utils

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.auth.dto.request.UserLoginRequest
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.testutils.UnitTest
import com.flab.ticketing.common.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.InputStream
import java.nio.charset.StandardCharsets

class UserLoginInfoConverterTest : UnitTest() {

    private val objectMapper: ObjectMapper = mockk()
    private val userLoginInfoConverter = UserLoginInfoConverter(objectMapper)

    init {

        "UserLoginDTO에 맞는 json 문자열과 이에 맞는 Charset이 들어올 경우 정상적으로 객체를 변환할 수 있다." {
            val email = "email@email.com"
            val userPW = "abc1234!"
            val charSet = StandardCharsets.UTF_8

            val inputStream = """{email : "$email", password : "$userPW"}""".byteInputStream(charSet)
            every { objectMapper.readValue(any<String>(), UserLoginRequest::class.java) } returns mockk()

            userLoginInfoConverter.convert(inputStream, charSet)

            verify { objectMapper.readValue(any<String>(), UserLoginRequest::class.java) }
        }

        "RequestBody의 문자열이 올바르지 않은 경우 InvalidValueException을 throw 한다." {
            val charSet = StandardCharsets.UTF_8

            val inputStream = """{"hello" : "world"}""".byteInputStream(charSet)
            every {
                objectMapper.readValue(
                    any<String>(),
                    UserLoginRequest::class.java
                )
            } throws JsonMappingException("에러 메시지")


            val e = shouldThrow<InvalidValueException> {
                userLoginInfoConverter.convert(inputStream, charSet)
            }

            e.info shouldBeEqual AuthErrorInfos.LOGIN_INFO_INVALID
        }

        "RequestBody가 아무것도 들어오지 않은 경우 InvalidValueException을 throw 한다." {
            val inputStream = InputStream.nullInputStream()
            val charSet = StandardCharsets.UTF_8

            val e = shouldThrow<InvalidValueException> {
                userLoginInfoConverter.convert(inputStream, charSet)
            }

            e.info shouldBeEqual AuthErrorInfos.LOGIN_INFO_INVALID
        }
    }

}