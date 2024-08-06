package com.flab.ticketing.user.utils

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.user.dto.UserLoginDto
import com.flab.ticketing.user.exception.UserErrorInfos
import org.springframework.util.StreamUtils
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

class UserLoginInfoConverter(
    private val objectMapper: ObjectMapper
) {

    fun convert(inputStream: InputStream, charset: Charset): UserLoginDto {
        try {
            val reqBody = StreamUtils.copyToString(inputStream, charset)

            return objectMapper.readValue(reqBody, UserLoginDto::class.java)
        } catch (e: JsonMappingException) {
            throw InvalidValueException(UserErrorInfos.LOGIN_INFO_INVALID)
        } catch (e: IOException) {
            throw InvalidValueException(UserErrorInfos.LOGIN_INFO_INVALID)
        }

    }

}