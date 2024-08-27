package com.flab.ticketing.order.service.client

import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

@Component
class TossPaymentClient(
    private val restClient: RestClient,
    @Value("\${service.toss.url}") private val tossServerUrl: String,
    @Value("\${service.toss.confirm-uri}") private val confirmUri: String,
    @Value("\${service.toss.access-token}") private val tossAccessToken: String
) {

    fun confirm(orderConfirmRequest: OrderConfirmRequest) {
        val tossOrderConfirmRequest = TossOrderConfirmRequest(
            orderId = orderConfirmRequest.orderId,
            paymentKey = orderConfirmRequest.paymentKey,
            amount = orderConfirmRequest.amount
        )

        val retrieve = restClient.post()
            .uri(URI.create("$tossServerUrl$confirmUri"))
            .header(HttpHeaders.AUTHORIZATION, "Basic $tossAccessToken")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(tossOrderConfirmRequest)
            .retrieve()
            .toBodilessEntity()
        println(retrieve.statusCode)
    }


    data class TossOrderConfirmRequest(
        val orderId: String,
        val paymentKey: String,
        val amount: Int
    )
}