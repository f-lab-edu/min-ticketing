package com.flab.ticketing.user.controller

import com.flab.ticketing.common.dto.ErrorResponse
import com.flab.ticketing.user.exception.DuplicatedEmailException
import com.flab.ticketing.user.exception.InvalidEmailException
import com.flab.ticketing.user.exception.UserExceptionMessages
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
}