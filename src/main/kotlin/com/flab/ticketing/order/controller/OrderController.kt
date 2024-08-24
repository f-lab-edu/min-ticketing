package com.flab.ticketing.order.controller

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.resolver.annotation.LoginUser
import com.flab.ticketing.order.dto.response.CartListResponse
import com.flab.ticketing.order.service.ReservationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val reservationService: ReservationService
) {

    @GetMapping("/carts")
    fun getCarts(@LoginUser userInfo: AuthenticatedUserDto): CartListResponse {
        return reservationService.getCarts(userInfo.uid)
    }

}