package com.flab.ticketing.order.service

import com.flab.ticketing.common.exception.DuplicatedException
import com.flab.ticketing.common.utils.NanoIdGenerator
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.exception.OrderErrorInfos
import com.flab.ticketing.order.repository.reader.ReservationReader
import com.flab.ticketing.order.repository.writer.CartWriter
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
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
    private val cartWriter: CartWriter,
    private val nanoIdGenerator: NanoIdGenerator

) {

    fun reserve(
        userUid: String,
        performanceUid: String,
        dateUid: String,
        seatUid: String
    ) {
        val user = userReader.findByUid(userUid)
        val performance = performanceReader.findPerformanceEntityByUidJoinWithPlace(performanceUid)

        val performanceDateTime = performanceReader.findDateEntityByUid(performanceUid, dateUid)
        val seat = performance.performancePlace.findSeatIn(seatUid)

        if (reservationReader.isReservationExists(seatUid, dateUid)) {
            throw DuplicatedException(OrderErrorInfos.ALREADY_RESERVED)
        }

        saveProcess(user, performanceDateTime, seat)
    }


    private fun saveProcess(
        user: User,
        performanceDateTime: PerformanceDateTime,
        seat: PerformancePlaceSeat
    ) {
        try {
            cartWriter.save(Cart(nanoIdGenerator.createNanoId(), seat, performanceDateTime, user))
        } catch (e: DataIntegrityViolationException) {
            throw DuplicatedException(OrderErrorInfos.ALREADY_RESERVED)
        }
    }
}