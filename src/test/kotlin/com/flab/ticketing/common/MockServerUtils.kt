package com.flab.ticketing.common

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.boot.test.context.TestComponent
import org.springframework.http.HttpStatus

@TestComponent
class MockServerUtils {

    private val mockWebServer = MockWebServer()

    @PostConstruct
    fun runServer() {
        mockWebServer.start(port = 1010)
    }

    fun getHost(): String {
        return "${mockWebServer.hostName}:${mockWebServer.port}"
    }

    fun addMockResponse(responseStatus: HttpStatus, body: String) {
        mockWebServer.enqueue(MockResponse().setResponseCode(responseStatus.value()).setBody(body))
    }

    @PreDestroy
    fun shutDown() {
        mockWebServer.shutdown()
    }
}