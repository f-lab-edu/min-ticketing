package com.flab.ticketing.order.repository.reader

import com.flab.ticketing.order.repository.ReservationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
@Transactional(readOnly = true)
class ReservationReader(
    private val reservationRepository: ReservationRepository
) {

    fun findReserveUidInPlace(performancePlaceId: Long, dateUid: String): List<String> {
        return reservationRepository.findReservedSeatUids(performancePlaceId, dateUid)
    }

    fun isReservationExists(seatUid: String, dateUid: String): Boolean {
        return reservationRepository.findReservationBySeatUidAndDateUid(seatUid, dateUid)?.let { true } ?: false
    }
}