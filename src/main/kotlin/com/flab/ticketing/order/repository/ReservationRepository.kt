package com.flab.ticketing.order.repository

import com.flab.ticketing.order.entity.Reservation
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface ReservationRepository : CrudRepository<Reservation, Long> {

    @Query(
        "SELECT r.seat.uid FROM Reservation r " +
                "WHERE r.performanceDateTime.uid = :dateUid " +
                "AND r.seat.place.id = :placeId"
    )
    fun findReservatedSeatUids(@Param("placeId") id: Long, @Param("dateUid") dateUid: String): List<String>
}