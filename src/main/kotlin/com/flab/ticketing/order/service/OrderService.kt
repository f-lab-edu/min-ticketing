package com.flab.ticketing.order.service

import com.flab.ticketing.auth.dto.service.AuthenticatedUserDto
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.order.dto.request.OrderInfoRequest
import com.flab.ticketing.order.dto.response.OrderInfoResponse
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.order.entity.Reservation
import com.flab.ticketing.order.repository.reader.CartReader
import com.flab.ticketing.order.repository.writer.CartWriter
import com.flab.ticketing.order.repository.writer.OrderWriter
import com.flab.ticketing.user.repository.reader.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional
class OrderService(
    private val userReader: UserReader,
    private val cartReader: CartReader,
    private val cartWriter: CartWriter,
    private val orderWriter: OrderWriter,
    private val nanoIdGenerator: NanoIdGenerator
) {

    fun saveRequestedOrderInfo(
        userInfo: AuthenticatedUserDto,
        orderInfoRequest: OrderInfoRequest
    ): OrderInfoResponse {

        val user = userReader.findByUid(userInfo.uid)
        val carts = cartReader.findByUidList(orderInfoRequest.carts)

        val order = Order(
            nanoIdGenerator.createNanoId(),
            user,
            payment = Order.Payment(
                carts.map { it.performanceDateTime.performance.price }.sum(),
                orderInfoRequest.payType
            )
        )

        carts.forEach {
            order.addReservation(Reservation(it.performanceDateTime, it.seat, order))
        }

        orderWriter.save(order)
        cartWriter.deleteAll(carts)

        return OrderInfoResponse(
            order.name,
            order.uid,
            user.email,
            user.nickname,
            order.payment.totalPrice
        )
    }

}