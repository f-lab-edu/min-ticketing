package com.flab.ticketing.common.exception

class NotFoundException(override val info: ErrorInfo)
    : BusinessException(info)