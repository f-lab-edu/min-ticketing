package com.flab.ticketing.common.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class CommonController {

    @GetMapping("/api/health-check")
    fun healthCheck(): ResponseEntity<String> {
        return ResponseEntity("Hello World", HttpStatus.OK)
    }

}