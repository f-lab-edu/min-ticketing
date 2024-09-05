package com.flab.ticketing.order.controller

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.resolver.annotation.LoginUser
import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.response.CartListResponse
import com.flab.ticketing.order.dto.response.OrderInfoResponse
import com.flab.ticketing.order.dto.response.OrderSummarySearchResult
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

    @PostMapping("/toss/confirm")
    fun confirmOrder(
        @LoginUser userInfo: AuthenticatedUserDto,
        @RequestBody orderConfirmRequest: OrderConfirmRequest
    ) {
        orderService.confirmOrder(userInfo.uid, orderConfirmRequest)
    }

    @GetMapping("/carts")
    fun getCarts(@LoginUser userInfo: AuthenticatedUserDto): CartListResponse {
        return reservationService.getCarts(userInfo.uid)
    }

    @GetMapping("")
    fun getOrderList(
        @LoginUser userInfo: AuthenticatedUserDto,
        @ModelAttribute cursorInfo: CursorInfoDto
    ): CursoredResponse<OrderSummarySearchResult>{
        val orderList = orderService.getOrderList(userInfo.uid, CursorInfoDto(cursorInfo.cursor, cursorInfo.limit + 1))

        if(orderList.size < cursorInfo.limit){
            return CursoredResponse(null, orderList)
        }
        return CursoredResponse(orderList[orderList.size-1].uid, orderList.dropLast(1))
    }

}