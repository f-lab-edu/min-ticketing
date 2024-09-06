package com.flab.ticketing.common

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.boot.test.context.TestComponent
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

@TestComponent
class MockServerUtils {

    private val mockWebServer = MockWebServer()

    @PostConstruct
    fun runServer() {
        mockWebServer.start(port = 2024)
    }

    fun getHost(): String {
        return "${mockWebServer.hostName}:${mockWebServer.port}"
    }

    fun addMockResponse(responseStatus: HttpStatus, body: String) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(responseStatus.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(body))
    }

    @PreDestroy
    fun shutDown() {
        mockWebServer.shutdown()
    }
}