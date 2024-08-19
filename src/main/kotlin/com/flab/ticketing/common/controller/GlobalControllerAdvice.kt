package com.flab.ticketing.common.controller

import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.common.exception.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
class GlobalControllerAdvice {

    @ExceptionHandler(DuplicatedException::class)
    fun handleDuplicatedException(e: BusinessException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse.of(e.info), HttpStatus.CONFLICT)
    }

    @ExceptionHandler(InvalidValueException::class, BusinessIllegalStateException::class, BadRequestException::class)
    fun handleBadRequestException(e: BusinessException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse.of(e.info), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: BusinessException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse.of(e.info), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleSpringValidatorException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errorFields = e.fieldErrors.asSequence()
            .map { it.field }
            .sorted()  // 알파벳 순으로 정렬
            .joinToString(",")

        val errorInfo = CommonErrorInfos.INVALID_FIELD
        return ResponseEntity(
            ErrorResponse("$errorFields${errorInfo.message}", errorInfo.code),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(e: BusinessException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse.of(e.info), HttpStatus.FORBIDDEN)
    }
}