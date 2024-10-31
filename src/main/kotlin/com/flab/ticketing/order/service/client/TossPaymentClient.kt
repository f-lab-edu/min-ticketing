package com.flab.ticketing.order.service.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.common.exception.CommonErrorInfos
import com.flab.ticketing.common.exception.ExternalAPIException
import com.flab.ticketing.common.exception.InternalServerException
import com.flab.ticketing.common.utils.Base64Utils
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.service.TossPayConfirmResponse
import com.flab.ticketing.order.dto.service.TossPayErrorResponse
import com.flab.ticketing.order.enums.TossPayConfirmErrorCode
import com.flab.ticketing.order.enums.TossPayErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

@Component
@Logging
class TossPaymentClient(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    @Value("\${service.toss.url}") private val tossServerUrl: String,
    @Value("\${service.toss.confirm-uri}") private val confirmUri: String,
    @Value("\${service.toss.cancel-uri}") private val cancelUriFormat: String,
    @Value("\${service.toss.access-token}") private val tossAccessToken: String
) {

    companion object {
        val TOSS_EXCEPTION_PREFIX = "토스 결제 오류 : "
    }

    private val log = LoggerFactory.getLogger(TossPaymentClient::class.java)

    fun confirm(orderConfirmRequest: OrderConfirmRequest): TossPayConfirmResponse {
        val tossOrderConfirmRequest = TossOrderConfirmRequest(
            orderId = orderConfirmRequest.orderId,
            paymentKey = orderConfirmRequest.paymentKey,
            amount = orderConfirmRequest.amount
        )

        val response = restClient.post()
            .uri(URI.create("$tossServerUrl$confirmUri"))
            .header(HttpHeaders.AUTHORIZATION, "Basic ${Base64Utils.encode(tossAccessToken + ":")}")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(tossOrderConfirmRequest)
            .retrieve()
            .checkError(TossPayErrorCode.Types.CONFIRM)
            .body(TossPayConfirmResponse::class.java)


        if (response == null) {
            log.warn("토스 페이먼츠 API가 정상 처리 되었으나 엔티티 변환에 실패했습니다.")
            throw InternalServerException(CommonErrorInfos.EXTERNAL_API_ERROR)
        }

        return response
    }

    fun cancel(paymentKey: String, reason: String) {
        restClient.post()
            .uri(URI.create("$tossServerUrl${String.format(cancelUriFormat, paymentKey)}"))
            .header(HttpHeaders.AUTHORIZATION, "Basic ${Base64Utils.encode(tossAccessToken + ":")}")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(TossOrderCancelRequest(reason))
            .retrieve()
            .checkError(TossPayErrorCode.Types.CANCEL)
            .toBodilessEntity()
    }


    private fun RestClient.ResponseSpec.checkError(type: String): RestClient.ResponseSpec {
        return this.onStatus(HttpStatusCode::isError) { _, res ->
            val resBody = runCatching {
                val s = convertDeserializableTossPayErrorResponse(res.body.readAllBytes(), type)
                objectMapper.readValue(s, TossPayErrorResponse::class.java)

            }.getOrElse {
                log.warn("Toss Payments 응답 객체 반환 중 오류가 발생하였습니다.")
                throw ExternalAPIException(
                    TossPayConfirmErrorCode.UNKNOWN_TOSS_MESSAGE.responseStatus,
                    "내부 서버 처리 중 오류가 있습니다."
                )
            }

            throw ExternalAPIException(resBody.code.responseStatus, TOSS_EXCEPTION_PREFIX + resBody.message)
        }


    }

    private fun convertDeserializableTossPayErrorResponse(responseBody: ByteArray, type: String): String? {
        val map = objectMapper.readValue(responseBody, Map::class.java).toMutableMap()
        val code = map.replace("code", listOf(type, map["code"]))

        if (code !is String) {
            throw ExternalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 처리 중 오류가 발생했습니다.")
        }

        return objectMapper.writeValueAsString(map)
    }


    internal data class TossOrderConfirmRequest(
        val orderId: String,
        val paymentKey: String,
        val amount: Int
    )

    internal data class TossOrderCancelRequest(
        val cancelReason: String
    )

}