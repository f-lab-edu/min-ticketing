package com.flab.ticketing.order.service

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.common.exception.InvalidValueException
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.order.dto.request.OrderConfirmRequest
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.response.OrderInfoResponse
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.Reservation
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.reader.CartReader
import com.flab.ticketing.order.repository.reader.OrderReader
import com.flab.ticketing.order.repository.writer.CartWriter
import com.flab.ticketing.order.repository.writer.OrderWriter
import com.flab.ticketing.order.service.client.TossPaymentClient
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.reader.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional
class OrderService(
    private val userReader: UserReader,
    private val cartReader: CartReader,
    private val cartWriter: CartWriter,
    private val orderReader: OrderReader,
    private val orderWriter: OrderWriter,
    private val nanoIdGenerator: NanoIdGenerator,
    private val tossPaymentClient: TossPaymentClient
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
        return OrderInfoResponse.of(user, order)
    }

    fun confirmOrder(userUid: String, orderConfirmRequest: OrderConfirmRequest) {
        val order = orderReader.findByUid(orderConfirmRequest.orderId)

        tossPaymentClient.confirm(orderConfirmRequest)
        
        order.status = Order.OrderStatus.COMPLETED
    }


    private fun checkValidOrderRequest(orderInfoRequest: OrderInfoRequest, carts: List<Cart>) {
        if (orderInfoRequest.carts.size != carts.size) {
            throw InvalidValueException(OrderErrorInfos.INVALID_CART_INFO)
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


}