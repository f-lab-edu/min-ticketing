package com.flab.ticketing.common.dto

data class CursoredResponse<T>(
    val cursor: String?,
    val data: List<T>
)