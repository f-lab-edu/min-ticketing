package com.flab.ticketing.common

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.boot.test.context.TestComponent
import org.springframework.http.HttpStatus

@TestComponent
class MockServerUtils {

    private val mockWebServer = MockWebServer()

    fun runServer() {
        mockWebServer.start()
    }

    fun getHost(): String {
        return "${mockWebServer.hostName}:${mockWebServer.port}"
    }

    fun addMockResponse(responseStatus: HttpStatus, body: String) {
        mockWebServer.enqueue(MockResponse().setResponseCode(responseStatus.value()).setBody(body))
    }

    fun shutDown() {
        mockWebServer.shutdown()
    }
}