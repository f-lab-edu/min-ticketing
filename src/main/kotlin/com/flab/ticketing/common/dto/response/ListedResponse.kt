package com.flab.ticketing.common.dto.response

data class ListedResponse<T>(
    val data: List<T>
)