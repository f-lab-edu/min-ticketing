package com.flab.ticketing.order.controller

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.resolver.annotation.LoginUser
import com.flab.ticketing.order.service.ReservationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {

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