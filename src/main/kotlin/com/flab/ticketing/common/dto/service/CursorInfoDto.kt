package com.flab.ticketing.common.dto.service

data class CursorInfoDto(
    val cursor: String? = null,
    val limit: Int = 10
)