package com.flab.ticketing.common.exception

abstract class BusinessException(open val info: ErrorInfo)
    : RuntimeException()