package com.flab.ticketing.common.exception

import org.springframework.http.HttpStatus

class ExternalAPIException(
    val status: HttpStatus,
    val returnMessage: String
) : RuntimeException()