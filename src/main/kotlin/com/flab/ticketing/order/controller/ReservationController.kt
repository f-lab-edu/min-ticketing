package com.flab.ticketing.order.controller

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.resolver.annotation.LoginUser
import com.flab.ticketing.common.dto.response.ErrorResponse
import com.flab.ticketing.order.service.ReservationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {


    @Operation(
        summary = "좌석 예약",
        description = "특정 공연의 특정 날짜와 좌석을 예약합니다.",
        responses = [
            ApiResponse(
                responseCode = "201", description = "예약 성공"
            ),
            ApiResponse(
                responseCode = "409", description = "이미 예약된 좌석 - ORDER-001",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/{performanceUid}/dates/{dateUid}/seats/{seatUid}")
    @ResponseStatus(HttpStatus.CREATED)
    fun reserve(
        @LoginUser user: AuthenticatedUserDto,
        @PathVariable performanceUid: String,
        @PathVariable dateUid: String,
        @PathVariable seatUid: String
    ) {
        reservationService.reserve(
            user.uid,
            performanceUid,
            dateUid,
            seatUid
        )
    }
}