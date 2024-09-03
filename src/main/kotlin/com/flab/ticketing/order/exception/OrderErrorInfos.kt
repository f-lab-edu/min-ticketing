package com.flab.ticketing.order.exception

import com.flab.ticketing.common.exception.ErrorInfo

enum class OrderErrorInfos(override val code: String, override val message: String) : ErrorInfo {

    ALREADY_RESERVED("ORDER-001", "이미 예약되었습니다."),
    INVALID_CART_INFO("ORDER-002", "올바르지 않은 장바구니 정보가 존재합니다."),
    ORDER_INFO_NOT_FOUND("ORDER-003", "주문 정보를 조회할 수 없습니다."),
    INVALID_USER("ORDER-004", "주문자와 동일하지 않은 사용자입니다."),
    ORDER_MUST_MINIMUM_ONE_RESERVATION("ORDER-005", "주문 시에는 최소 하나 이상의 주문 상품이 필요합니다."),
    INVALID_ORDER("ORDER-006", "주문의 상태가 올바르지 않습니다.")

}