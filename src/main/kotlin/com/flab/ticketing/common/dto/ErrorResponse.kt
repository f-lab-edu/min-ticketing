package com.flab.ticketing.common.dto

import com.flab.ticketing.common.exception.ErrorInfo

data class ErrorResponse(
    val message: String,
    val code: String
) {

    constructor(errorInfo: ErrorInfo)
            : this(message = errorInfo.message, code = errorInfo.code)

}