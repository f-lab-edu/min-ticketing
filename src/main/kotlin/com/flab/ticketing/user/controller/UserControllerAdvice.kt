package com.flab.ticketing.user.controller

import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.user.exception.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice



@RestControllerAdvice
class UserControllerAdvice {


    @ExceptionHandler(DuplicatedEmailException::class)
    fun duplicatedEmail() : ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(UserExceptionMessages.DUPLICATED_EMAIL.message), HttpStatus.CONFLICT)
    }

    @ExceptionHandler(InvalidEmailException::class)
    fun invalidEmail() : ResponseEntity<ErrorResponse>{
        return ResponseEntity(ErrorResponse(UserExceptionMessages.EMAIL_EXPRESSION_INVALID.message), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NotFoundEmailCodeException::class)
    fun notFoundEmailVerifyCode() : ResponseEntity<ErrorResponse>{
        return ResponseEntity(ErrorResponse(UserExceptionMessages.EMAIL_VERIFYCODE_NOT_FOUND.message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidEmailCodeException::class)
    fun invalidEmailVerifyCode() : ResponseEntity<ErrorResponse>{
        return ResponseEntity(ErrorResponse(UserExceptionMessages.EMAIL_VERIFYCODE_INVALID.message), HttpStatus.BAD_REQUEST)
    }

}