package com.flab.ticketing.common.dto.response

data class CursoredResponse<T>(
    val cursor: String?,
    val data: List<T>
)