package com.flab.ticketing.common.controller

import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.exception.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
class GlobalControllerAdvice {

    @ExceptionHandler(DuplicatedException::class)
    fun duplicatedEmail(e: DuplicatedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(e.info), HttpStatus.CONFLICT)
    }

    @ExceptionHandler(InvalidValueException::class)
    fun invalidEmail(e: InvalidValueException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(e.info), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NotFoundException::class)
    fun notFoundEmailVerifyCode(e: NotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(e.info), HttpStatus.NOT_FOUND)
    }


}