package com.flab.ticketing.common.exception

abstract class BusinessException : RuntimeException{
    abstract val info: ErrorInfo
    constructor()

    constructor(cause: Throwable) : super(cause)
}