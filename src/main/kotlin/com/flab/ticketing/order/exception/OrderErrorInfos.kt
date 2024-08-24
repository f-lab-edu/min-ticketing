package com.flab.ticketing.order.exception

import com.flab.ticketing.common.exception.ErrorInfo

enum class OrderErrorInfos(override val code: String, override val message: String) : ErrorInfo {

    ALREADY_RESERVATED("ORDER-001", "이미 예약되었습니다.")

}