package com.flab.ticketing.order.controller

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.resolver.annotation.LoginUser
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.response.CartListResponse
import com.flab.ticketing.order.dto.response.OrderInfoResponse
import com.flab.ticketing.order.service.OrderService
import com.flab.ticketing.order.service.ReservationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val reservationService: ReservationService,
    private val orderService: OrderService
) {

    @PostMapping("/toss/info")
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrderRequest(
        @LoginUser userInfo: AuthenticatedUserDto,
        @RequestBody orderInfoRequest: OrderInfoRequest
    ): OrderInfoResponse {
        return orderService.saveRequestedOrderInfo(userInfo, orderInfoRequest)
    }


    @GetMapping("/carts")
    fun getCarts(@LoginUser userInfo: AuthenticatedUserDto): CartListResponse {
        return reservationService.getCarts(userInfo.uid)
    }

}