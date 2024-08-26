package com.flab.ticketing.order.repository

import com.flab.ticketing.order.entity.Cart
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CartRepository : JpaRepository<Cart, Long> {

    fun save(cart: Cart)

    @Query("SELECT c FROM Cart c WHERE c.user.uid = :userUid")
    fun findByUserUid(@Param("userUid") userUid: String): List<Cart>

    @Query("SELECT c FROM Cart c WHERE c.performanceDateTime.uid = :dateUid AND c.seat.uid = :seatUid")
    fun findByDateUidAndSeatUid(@Param("dateUid") dateUid: String, @Param("seatUid") seatUid: String): Cart?

    @Query(
        "SELECT c.seat.uid FROM Cart c " +
                "WHERE c.performanceDateTime.uid = :dateUid " +
                "AND EXISTS(" +
                " SELECT 1 FROM PerformancePlaceSeat s WHERE s = c.seat " +
                " AND s.place.id = :placeId)"
    )
    fun findSeatUidByDateUidAndPlaceIn(@Param("dateUid") dateUid: String, @Param("placeId") placeId: Long): List<String>
}