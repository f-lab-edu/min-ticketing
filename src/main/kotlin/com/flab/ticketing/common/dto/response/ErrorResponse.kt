package com.flab.ticketing.common.dto.response

import com.flab.ticketing.common.exception.ErrorInfo

data class ErrorResponse(
    val message: String,
    val code: String
) {

    companion object {
        fun of(errorInfo: ErrorInfo): ErrorResponse {
            return ErrorResponse(message = errorInfo.message, code = errorInfo.code)

        }

    }

}