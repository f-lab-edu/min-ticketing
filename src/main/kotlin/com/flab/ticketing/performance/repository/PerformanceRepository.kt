package com.flab.ticketing.performance.repository

import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.performance.dto.service.PerformanceDetailSearchResult
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.repository.dsl.CustomPerformanceRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PerformanceRepository : CustomPerformanceRepository,
    org.springframework.data.repository.Repository<Performance, Long> {
    fun save(performance: Performance)

    fun deleteAll()


    @Query(
        "SELECT new com.flab.ticketing.performance.dto.service.PerformanceDetailSearchResult(p.uid, p.image, p.name, r.name, pp.name, p.price, p.description) FROM Performance p " +
                "JOIN p.performancePlace pp " +
                "JOIN pp.region r " +
                "WHERE p.uid = :uid"
    )
    fun findByUid(@Param("uid") uid: String): PerformanceDetailSearchResult?


    @Query(
        "SELECT new com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult(pd.uid, pd.showTime, count(ss), count(rs.seat)) FROM Performance p " +
                "JOIN p.performanceDateTime pd " +
                "JOIN p.performancePlace pp " +
                "JOIN pp.seats ss " +
                "LEFT JOIN Reservation rs ON ss = rs.seat " +
                "WHERE p.uid = :uid " +
                "GROUP BY pd.uid"
    )
    fun getDateInfo(@Param("uid") performanceUid: String): List<PerformanceDateSummaryResult>


    @Query(
        "SELECT p From Performance p " +
                "JOIN FETCH p.performancePlace pp " +
                "JOIN FETCH pp.seats " +
                "WHERE p.uid = :uid"
    )
    fun findPerformanceByUidJoinWithPlaceAndSeat(@Param("uid") uid: String): Performance?


    @Query(
        "SELECT pd FROM PerformanceDateTime pd " +
                "WHERE pd.performance.uid = :performanceUid " +
                "AND pd.uid = :dateUid"
    )
    fun findPerformanceDateTime(
        @Param("performanceUid") performanceUid: String,
        @Param("dateUid") dateUid: String
    ): PerformanceDateTime?

}