package com.flab.ticketing.order.exception

import com.flab.ticketing.common.exception.ErrorInfo

enum class OrderErrorInfos(override val code: String, override val message: String) : ErrorInfo {

    ALREADY_RESERVED("ORDER-001", "이미 예약되었습니다."),
    INVALID_CART_INFO("ORDER-002", "올바르지 않은 장바구니 정보가 존재합니다."),
    ORDER_INFO_NOT_FOUND("ORDER-003", "주문 정보를 조회할 수 없습니다."),
    INVALID_USER("ORDER-004", "현재 로그인된 유저와 주문 정보가 일치하지 않습니다.")

}