package com.flab.ticketing.common.exception

class DuplicatedException(override val info: ErrorInfo) : BusinessException(info)