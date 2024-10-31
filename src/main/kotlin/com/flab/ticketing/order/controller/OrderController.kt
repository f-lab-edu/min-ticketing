package com.flab.ticketing.order.controller

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.auth.resolver.annotation.LoginUser
import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.common.dto.response.ErrorResponse
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.request.OrderSearchConditions
import com.flab.ticketing.order.dto.response.CartListResponse
import com.flab.ticketing.order.dto.response.OrderInfoResponse
import com.flab.ticketing.order.dto.response.OrderSummarySearchResult
import com.flab.ticketing.order.enums.OrderCancelReasons
import com.flab.ticketing.order.service.OrderService
import com.flab.ticketing.order.service.ReservationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val reservationService: ReservationService,
    private val orderService: OrderService
) {
    @Operation(
        summary = "주문 정보 생성",
        description = "장바구니 항목을 기반으로 주문 정보를 생성합니다.",
        responses = [
            ApiResponse(responseCode = "201", description = "주문 정보 생성 성공"),
            ApiResponse(
                responseCode = "400", description = "잘못된 Cart UID 포함 - ORDER-002",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/toss/info")
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrderRequest(
        @LoginUser userInfo: AuthenticatedUserDto,
        @RequestBody orderInfoRequest: OrderInfoRequest
    ): OrderInfoResponse {
        return orderService.createOrderMetaData(userInfo, orderInfoRequest)
    }


    @Operation(
        summary = "주문 확인",
        description = "생성된 주문을 확인하고 결제를 진행합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "결제 승인 성공"),
            ApiResponse(responseCode = "400", description = "토스 페이 API 정상 응답이 아닌 경우 - COMMON-004"),
            ApiResponse(responseCode = "401", description = "토스 페이 API 정상 응답이 아닌 경우 - COMMON-004"),
            ApiResponse(responseCode = "403", description = "토스 페이 API 정상 응답이 아닌 경우 - COMMON-004"),
            ApiResponse(responseCode = "404", description = "토스 페이 API 정상 응답이 아닌 경우 - COMMON-004"),
            ApiResponse(responseCode = "500", description = "토스 페이 API 정상 응답이 아닌 경우 - COMMON-004"),
            ApiResponse(
                responseCode = "502", description = "토스 페이 API 정상 응답이 아닌 경우 - COMMON-004",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(responseCode = "403", description = "주문자와 동일하지 않은 호출자 - ORDER-004"),
            ApiResponse(responseCode = "404", description = "주문 정보 없음")
        ]
    )
    @PostMapping("/toss/confirm")
    fun confirmOrder(
        @LoginUser userInfo: AuthenticatedUserDto,
        @RequestBody orderConfirmRequest: OrderConfirmRequest
    ) {
        orderService.confirmOrder(userInfo.uid, orderConfirmRequest)
    }

    @Operation(
        summary = "장바구니 조회",
        description = "사용자의 장바구니 내용을 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "장바구니 조회 성공")
        ]
    )
    @GetMapping("/carts")
    fun getCarts(@LoginUser userInfo: AuthenticatedUserDto): CartListResponse {
        return reservationService.getCarts(userInfo.uid)
    }

    @Operation(
        summary = "주문 목록 조회",
        description = "사용자의 주문 목록을 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "주문 목록 조회 성공")
        ]
    )
    @GetMapping("")
    fun getOrderList(
        @Parameter(hidden = true) @LoginUser userInfo: AuthenticatedUserDto,
        @ParameterObject @ModelAttribute cursorInfo: CursorInfoDto,
        @ParameterObject @ModelAttribute searchConditions: OrderSearchConditions
    ): CursoredResponse<OrderSummarySearchResult> {
        val orderList = orderService.getOrderList(
            userInfo.uid,
            searchConditions,
            CursorInfoDto(cursorInfo.cursor, cursorInfo.limit + 1)
        )

        if (orderList.size < cursorInfo.limit) {
            return CursoredResponse(null, orderList)
        }
        return CursoredResponse(orderList[orderList.size - 1].uid, orderList.dropLast(1))
    }


    @Operation(
        summary = "주문 취소",
        description = "특정 주문을 취소합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "주문 취소 성공"),
            ApiResponse(responseCode = "400", description = "Toss API 오류 - COMMON-004"),
            ApiResponse(responseCode = "403", description = "Toss API 오류 - COMMON-004"),
            ApiResponse(responseCode = "500", description = "Toss API 오류 - COMMON-004"),
            ApiResponse(
                responseCode = "404", description = "주문 정보 없음"
            ),
            ApiResponse(
                responseCode = "502", description = "Toss API 오류 - COMMON-004",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    @PostMapping("/{orderUid}/cancel")
    fun cancelOrder(
        @LoginUser userInfo: AuthenticatedUserDto,
        @PathVariable orderUid: String
    ) {
        orderService.cancelOrder(userInfo.uid, orderUid, OrderCancelReasons.CUSTOMER_WANTS)
    }

}