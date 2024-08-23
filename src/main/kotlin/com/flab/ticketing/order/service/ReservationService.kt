package com.flab.ticketing.order.service

import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.reader.ReservationReader
import com.flab.ticketing.order.repository.writer.CartWriter
import com.flab.ticketing.performance.repository.reader.PerformanceReader
import com.flab.ticketing.user.entity.User
import com.flab.ticketing.user.repository.reader.UserReader
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service


@Service
class ReservationService(
    private val userReader: UserReader,
    private val reservationReader: ReservationReader,
    private val performanceReader: PerformanceReader,
    private val cartWriter: CartWriter
) {

    fun reserve(
        userUid: String,
        performanceUid: String,
        dateUid: String,
        seatUid: String
    ) {
        val user = userReader.findByUid(userUid)
        val performance = performanceReader.findPerformanceEntityByUidJoinWithPlace(performanceUid)

        performanceReader.findDateEntityByUid(performanceUid, dateUid)
        performance.performancePlace.checkSeatIn(seatUid)

        if (reservationReader.isReservationExists(seatUid, dateUid)) {
            throw DuplicatedException(OrderErrorInfos.ALREADY_RESERVED)
        }

        saveProcess(user, dateUid, seatUid)
    }


    private fun saveProcess(
        user: User,
        dateUid: String,
        seatUid: String
    ) {
        try {
            cartWriter.save(Cart(seatUid, dateUid, user))
        } catch (e: DataIntegrityViolationException) {
            throw DuplicatedException(OrderErrorInfos.ALREADY_RESERVED)
        }
    }
}