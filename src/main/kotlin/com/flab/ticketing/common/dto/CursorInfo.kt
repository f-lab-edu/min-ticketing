package com.flab.ticketing.common.dto

data class CursorInfo(
    val cursor: String? = null,
    val limit: Int = 10
)