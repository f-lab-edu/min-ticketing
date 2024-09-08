package com.flab.ticketing.order.dto.service

import com.flab.ticketing.order.enums.TossPayErrorCode
data class TossPayErrorResponse(
    val code: TossPayErrorCode,
    val message: String
)