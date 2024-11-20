package com.flab.ticketing.performance.repository

import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.repository.dsl.CustomPerformanceRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PerformanceRepository : CustomPerformanceRepository,
    CrudRepository<Performance, Long> {
    fun save(performance: Performance)
    
    fun findByUid(@Param("uid") uid: String): Performance?


    @Query(
        "SELECT p From Performance p " +
                "JOIN FETCH p.performancePlace pp " +
                "JOIN FETCH pp.seats " +
                "WHERE p.uid = :uid"
    )
    fun findPerformanceByUidJoinWithPlaceAndSeat(@Param("uid") uid: String): Performance?


}