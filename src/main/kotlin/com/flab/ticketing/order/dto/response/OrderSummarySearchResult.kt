package com.flab.ticketing.order.dto.response

import java.time.ZonedDateTime

data class OrderSummarySearchResult(
    val uid: String,
    val name : String,
    val performanceImage : String,
    val totalPrice : Int,
    val orderedTime: ZonedDateTime
)