package com.flab.ticketing.order.dto.request

data class OrderInfoRequest(
    val payType: String,
    val carts: List<String>
)