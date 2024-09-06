package com.flab.ticketing.order.service.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.ExternalAPIException
import com.flab.ticketing.common.exception.InternalServerException
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.service.TossPayConfirmResponse
import com.flab.ticketing.order.dto.service.TossPayErrorResponse
import com.flab.ticketing.order.enums.TossPayErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

@Component
class TossPaymentClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    @Value("\${service.toss.url}") private val tossServerUrl: String,
    @Value("\${service.toss.confirm-uri}") private val confirmUri: String,
    @Value("\${service.toss.access-token}") private val tossAccessToken: String
) {

    companion object {
        val TOSS_EXCEPTION_PREFIX = "토스 결제 오류 : "
    }

    private val log = LoggerFactory.getLogger(TossPayErrorResponse::class.java)

    fun confirm(orderConfirmRequest: OrderConfirmRequest): TossPayConfirmResponse {
        val tossOrderConfirmRequest = TossOrderConfirmRequest(
            orderId = orderConfirmRequest.orderId,
            paymentKey = orderConfirmRequest.paymentKey,
            amount = orderConfirmRequest.amount
        )

        val response = restClient.post()
            .uri(URI.create("$tossServerUrl$confirmUri"))
            .header(HttpHeaders.AUTHORIZATION, "Basic $tossAccessToken")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(tossOrderConfirmRequest)
            .retrieve()
            .onStatus(HttpStatusCode::isError) { _, res ->
                val resBody = runCatching {
                    objectMapper.readValue(res.body.readAllBytes(), TossPayErrorResponse::class.java)
                }.getOrElse {
                    throw ExternalAPIException(TossPayErrorCode.UNKNOWN_TOSS_MESSAGE.responseStatus, "통신에 실패했습니다.")
                }

                throw ExternalAPIException(resBody.code.responseStatus, TOSS_EXCEPTION_PREFIX + resBody.message)
            }
            .body(TossPayConfirmResponse::class.java)


        if (response == null) {
            log.warn("토스 페이먼츠 API가 정상 처리 되었으나 엔티티 변환에 실패했습니다.")
            throw InternalServerException(CommonErrorInfos.EXTERNAL_API_ERROR)
        }

        return response
    }

    internal data class TossOrderConfirmRequest(
        val orderId: String,
        val paymentKey: String,
        val amount: Int
    )

}