package com.flab.ticketing.order.repository

import com.flab.ticketing.order.entity.Reservation
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

@org.springframework.stereotype.Repository
interface ReservationRepository  : Repository<Reservation, Long>{
    @Query("select count(r) from Reservation r JOIN r.performanceDateTime WHERE r.performanceDateTime.uid = :uid")
    fun countByPerformanceDateTime(@Param("uid") performanceUid : String) : Long
}