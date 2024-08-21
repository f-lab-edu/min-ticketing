package com.flab.ticketing.order.service.reader

import com.flab.ticketing.order.repository.ReservationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
@Transactional(readOnly = true)
class ReservationReader(
    private val reservationRepository: ReservationRepository
) {

    fun findReservateUidInPlace(performancePlaceId: Long, dateUid: String): List<String> {
        return reservationRepository.findReservatedSeatUids(performancePlaceId, dateUid)
    }

    fun isReservateExists(seatUid: String, dateUid: String): Boolean {
        return reservationRepository.findReservationBySeatUidAndDateUid(seatUid, dateUid)?.let { true } ?: false
    }
}