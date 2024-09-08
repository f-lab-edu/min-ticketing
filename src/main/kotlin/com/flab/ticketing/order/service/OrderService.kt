package com.flab.ticketing.order.service

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.exception.BadRequestException
import com.flab.ticketing.common.exception.ExternalAPIException
import com.flab.ticketing.common.exception.ForbiddenException
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.service.FileService
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.common.utils.QRCodeGenerator
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.response.OrderInfoResponse
import com.flab.ticketing.order.dto.response.OrderSummarySearchResult
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.Reservation
import com.flab.ticketing.order.enums.OrderCancelReasons
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.reader.CartReader
import com.flab.ticketing.order.repository.reader.OrderReader
import com.flab.ticketing.order.repository.writer.CartWriter
import com.flab.ticketing.order.repository.writer.OrderWriter
import com.flab.ticketing.order.service.client.TossPaymentClient
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.reader.UserReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime


@Service
@Transactional
class OrderService(
    private val userReader: UserReader,
    private val cartReader: CartReader,
    private val cartWriter: CartWriter,
    private val orderReader: OrderReader,
    private val orderWriter: OrderWriter,
    private val nanoIdGenerator: NanoIdGenerator,
    private val tossPaymentClient: TossPaymentClient,
    private val fileService: FileService,
    @Value("\${service.url}") private val serviceUrl: String
) {

    fun saveRequestedOrderInfo(
        userInfo: AuthenticatedUserDto,
        orderInfoRequest: OrderInfoRequest
    ): OrderInfoResponse {

        val user = userReader.findByUid(userInfo.uid)
        val carts = cartReader.findByUidList(orderInfoRequest.carts)

        checkValidOrderRequest(orderInfoRequest, carts)
        val order = createOrder(user, orderInfoRequest, carts)

        carts.forEach {
            order.addReservation(Reservation(it.performanceDateTime, it.seat, order))
        }

        orderWriter.save(order)
        cartWriter.deleteAll(carts)
        return OrderInfoResponse.of(user, order)
    }


    @Transactional(noRollbackFor = [ExternalAPIException::class])
    fun confirmOrder(userUid: String, orderConfirmRequest: OrderConfirmRequest) {
        val order = orderReader.findByUid(orderConfirmRequest.orderId)
        checkValidOrderConfirmRequest(userUid, order)

        runCatching {
            val apiResponse = tossPaymentClient.confirm(orderConfirmRequest)
            order.status = Order.OrderStatus.COMPLETED
            order.payment.paymentKey = apiResponse.paymentKey
            createReservationQRCode(order)
        }.onFailure {
            order.status = Order.OrderStatus.PENDING
            throw it
        }

    }


    @Transactional(readOnly = true)
    fun getOrderList(userUid: String, cursorInfo: CursorInfoDto): List<OrderSummarySearchResult> {
        val orders = orderReader.findOrderByUser(userUid, cursorInfo)

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

    fun cancelOrder(userUid: String, orderUid: String, reason: OrderCancelReasons) {
        val order = orderReader.findByUid(orderUid)

        checkValidOrderCancelRequest(userUid, order)

        tossPaymentClient.cancel(order.payment.paymentKey!!, reason.reason)

        order.status = Order.OrderStatus.CANCELED
    }


    private fun checkValidOrderRequest(orderInfoRequest: OrderInfoRequest, carts: List<Cart>) {
        if (orderInfoRequest.carts.size != carts.size) {
            throw InvalidValueException(OrderErrorInfos.INVALID_CART_INFO)
        }
    }

    private fun checkValidOrderConfirmRequest(userUid: String, order: Order) {
        checkUser(userUid, order.user.uid)
    }

    private fun checkValidOrderCancelRequest(userUid: String, order: Order) {
        checkUser(userUid, order.user.uid)
        checkPerformancePassed(order.reservations)
    }

    private fun checkUser(actualUserUid: String, expectedSameUserUid: String) {
        if (expectedSameUserUid != actualUserUid) {
            throw ForbiddenException(OrderErrorInfos.INVALID_USER)
        }
    }

    private fun checkPerformancePassed(reservations: List<Reservation>) {
        val now = ZonedDateTime.now()

        val passedReservation = reservations.filter {
            it.performanceDateTime.showTime.isBefore(now)
        }.firstOrNull()

        if (passedReservation != null) {
            throw BadRequestException(PerformanceErrorInfos.PERFORMANCE_ALREADY_PASSED)
        }
    }

    private fun createOrder(user: User, orderInfoRequest: OrderInfoRequest, carts: List<Cart>): Order {
        return Order(
            nanoIdGenerator.createNanoId(),
            user,
            payment = Order.Payment(
                carts.map { it.performanceDateTime.performance.price }.sum(),
                orderInfoRequest.payType
            )
        )
    }

    private fun createReservationQRCode(order: Order) {
        order.reservations.forEach {
            val qrCodeImage = QRCodeGenerator.gererateQR("$serviceUrl/orders/${it.id}/use")
            val savedImageUrl = fileService.uploadImage(qrCodeImage)
            it.qrImageUrl = savedImageUrl
        }

    }


}