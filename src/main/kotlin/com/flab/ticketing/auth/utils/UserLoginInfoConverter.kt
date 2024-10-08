package com.flab.ticketing.auth.utils

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.auth.dto.request.UserLoginRequest
import com.flab.ticketing.auth.exception.AuthErrorInfos
import com.flab.ticketing.common.exception.InvalidValueException
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset


@Component
class UserLoginInfoConverter(
    private val objectMapper: ObjectMapper
) {

    fun convert(inputStream: InputStream, charset: Charset): UserLoginRequest {
        try {
            val reqBody = StreamUtils.copyToString(inputStream, charset)

            return objectMapper.readValue(reqBody, UserLoginRequest::class.java)
        } catch (e: JsonMappingException) {
            throw InvalidValueException(AuthErrorInfos.LOGIN_INFO_INVALID)
        } catch (e: IOException) {
            throw InvalidValueException(AuthErrorInfos.LOGIN_INFO_INVALID)
        }

    }

}