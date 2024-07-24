package com.flab.ticketing.common.exception

class InvalidValueException(override val info: ErrorInfo)
    : BusinessException(info)