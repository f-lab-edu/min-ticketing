package com.flab.ticketing.performance.dto.response

data class PerformanceDateDetailResponse(
    val dateUid: String,
    val pricePerSeat: Int,
    val seats: List<List<SeatInfo>>
) {

    data class SeatInfo(
        val uid: String,
        val name: String,
        val isReserved: Boolean
    )

}