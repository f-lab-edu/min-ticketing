package com.flab.ticketing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class TicketingApplication

fun main(args: Array<String>) {
    runApplication<TicketingApplication>(*args)
}