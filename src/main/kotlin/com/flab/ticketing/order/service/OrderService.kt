package com.flab.ticketing.order.service

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.exception.BadRequestException
import com.flab.ticketing.common.exception.ForbiddenException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.service.FileService
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.common.utils.QRCodeGenerator
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.request.OrderSearchConditions
import com.flab.ticketing.order.dto.response.OrderDetailInfoResponse
import com.flab.ticketing.order.dto.response.OrderInfoResponse
import com.flab.ticketing.order.dto.response.OrderSummarySearchResult
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.OrderMetaData
import com.flab.ticketing.order.entity.Reservation
import com.flab.ticketing.order.enums.OrderCancelReasons
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.reader.CartReader
import com.flab.ticketing.order.repository.reader.OrderReader
import com.flab.ticketing.order.repository.writer.CartWriter
import com.flab.ticketing.order.repository.writer.OrderWriter
import com.flab.ticketing.order.service.client.TossPaymentClient
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.user.repository.reader.UserReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime


@Service
@Transactional
@Logging
class OrderService(
    private val userReader: UserReader,
    private val cartReader: CartReader,
    private val cartWriter: CartWriter,
    private val orderReader: OrderReader,
    private val orderWriter: OrderWriter,
    private val tossPaymentClient: TossPaymentClient,
    private val fileService: FileService,
    @Value("\${service.url}") private val serviceUrl: String
) {

    fun createOrderMetaData(
        userInfo: AuthenticatedUserDto,
        orderInfoRequest: OrderInfoRequest
    ): OrderInfoResponse {

        val user = userReader.findByUid(userInfo.uid)
        val carts = cartReader.findByUidList(orderInfoRequest.cartUidList, user)

        checkValidOrderRequest(orderInfoRequest, carts)

        val orderMetaData = OrderMetaData(
            orderId = NanoIdGenerator.createNanoId(),
            amount = carts.calculatePrice(),
            cartUidList = orderInfoRequest.cartUidList,
            userUid = user.uid
        )

        orderWriter.save(orderMetaData)

        return OrderInfoResponse(orderMetaData.orderId, orderMetaData.amount)
    }


    fun confirmOrder(userUid: String, orderConfirmRequest: OrderConfirmRequest) {
        val orderMetaData = orderReader.findMetaData(orderConfirmRequest.orderId)
        val user = userReader.findByUid(userUid)
        val cartList = cartReader.findByUidList(orderMetaData.cartUidList, user)
        checkValidOrderConfirmRequest(userUid, orderMetaData)

        val order = Order.of(
            metaData = orderMetaData,
            user = user,
            payment = orderConfirmRequest.ofPayments(),
            carts = cartList
        )

        orderWriter.deleteMetaData(orderMetaData)
        tossPaymentClient.confirm(orderConfirmRequest)
        createReservationQRCode(order)
        orderWriter.save(order)
        cartWriter.deleteAll(cartList)
    }


    @Transactional(readOnly = true)
    fun getOrderList(
        userUid: String,
        searchConditions: OrderSearchConditions,
        cursorInfo: CursorInfoDto
    ): List<OrderSummarySearchResult> {
        val orders = orderReader.findOrderByUser(userUid, searchConditions, cursorInfo)

        return orders.filter { it.reservations.size > 0 }.map {
            OrderSummarySearchResult(
                it.uid,
                it.name,
                it.reservations[0].performanceDateTime.performance.image,
                it.payment.totalPrice,
                it.createdAt
            )
        }
    }

    @Transactional(readOnly = true)
    fun getOrderDetail(orderUid: String, userUid: String): OrderDetailInfoResponse {
        val order = orderReader.findByUid(orderUid)
        
        return OrderDetailInfoResponse.of(order)
    }


    fun cancelOrder(
        userUid: String,
        orderUid: String,
        reason: OrderCancelReasons,
        compareTime: ZonedDateTime = ZonedDateTime.now()
    ) {
        val order = orderReader.findByUid(orderUid)

        checkValidOrderCancelRequest(userUid, order, compareTime)

        tossPaymentClient.cancel(order.payment.paymentKey, reason.reason)

        order.status = Order.OrderStatus.CANCELED
    }


    private fun checkValidOrderRequest(orderInfoRequest: OrderInfoRequest, carts: List<Cart>) {
        if (orderInfoRequest.cartUidList.size != carts.size) {
            throw InvalidValueException(OrderErrorInfos.INVALID_CART_INFO)
        }
    }

    private fun checkValidOrderConfirmRequest(userUid: String, order: OrderMetaData) {
        checkUser(userUid, order.userUid)
    }

    private fun checkValidOrderCancelRequest(userUid: String, order: Order, compareTime: ZonedDateTime) {
        checkUser(userUid, order.user.uid)
        checkPerformancePassed(order.reservations, compareTime)
        checkReservationUsed(order.reservations)
    }

    private fun checkUser(actualUserUid: String, expectedSameUserUid: String) {
        if (expectedSameUserUid != actualUserUid) {
            throw ForbiddenException(OrderErrorInfos.INVALID_USER)
        }
    }

    private fun checkPerformancePassed(reservations: List<Reservation>, compareTime: ZonedDateTime) {
        reservations.forEach {
            val showTime = it.performanceDateTime.showTime
            if (showTime.isBefore(compareTime)) {
                throw BadRequestException(PerformanceErrorInfos.PERFORMANCE_ALREADY_PASSED)
            }

        }


    }

    private fun checkReservationUsed(reservations: List<Reservation>) {
        reservations.forEach {
            if (it.isUsed) {
                throw ForbiddenException(OrderErrorInfos.RESERVATION_ALREADY_USED)
            }

        }
    }


    private fun createReservationQRCode(order: Order) {
        order.reservations.forEach {
            val qrCodeImage = QRCodeGenerator.gererateQR("$serviceUrl/orders/${it.id}/use")
            val savedImageUrl = fileService.uploadImage(qrCodeImage)
            it.qrImageUrl = savedImageUrl
        }

    }


    private fun List<Cart>.calculatePrice(): Int {
        return this.map { it.performanceDateTime.performance.price }.sum()
    }


}