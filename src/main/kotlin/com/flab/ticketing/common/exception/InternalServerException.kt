package com.flab.ticketing.common.exception

class InternalServerException : BusinessException{
    override val info: ErrorInfo

    constructor(errorInfo: ErrorInfo) : super() {
        this.info = errorInfo
    }

    constructor(errorInfo: ErrorInfo, cause: Throwable) : super(cause) {
        this.info = errorInfo
    }
}