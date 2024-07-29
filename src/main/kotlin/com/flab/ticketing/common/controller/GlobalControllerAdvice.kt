package com.flab.ticketing.common.controller

import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.exception.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.*


@RestControllerAdvice
class GlobalControllerAdvice {

    @ExceptionHandler(DuplicatedException::class)
    fun duplicatedEmail(e: DuplicatedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse.of(e.info), HttpStatus.CONFLICT)
    }

    @ExceptionHandler(InvalidValueException::class)
    fun invalidEmail(e: InvalidValueException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse.of(e.info), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NotFoundException::class)
    fun notFoundEmailVerifyCode(e: NotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse.of(e.info), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun springValidatorException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val sj = StringJoiner(",")

        e.fieldErrors.stream().forEach { s -> sj.add(s.field) }

        val errorInfo = CommonErrorInfos.INVALID_FIELD
        return ResponseEntity(ErrorResponse(sj.toString() + errorInfo.message, errorInfo.code), HttpStatus.BAD_REQUEST)
    }

}