package com.flab.ticketing.performance.repository

import com.flab.ticketing.performance.dto.PerformanceDateInfo
import com.flab.ticketing.performance.dto.PerformanceDetailSearchResult
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.repository.dsl.CustomPerformanceRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PerformanceRepository : CustomPerformanceRepository,
    org.springframework.data.repository.Repository<Performance, Long> {
    fun save(performance: Performance)

    fun deleteAll()


    @Query("SELECT new com.flab.ticketing.performance.dto.PerformanceDetailSearchResult(p.uid, p.image, p.name, r.name, pp.name, p.price, p.description) FROM Performance p " +
            "JOIN p.performancePlace pp " +
            "JOIN pp.region r")
    fun findByUid(uid : String): PerformanceDetailSearchResult?



    @Query("SELECT new com.flab.ticketing.performance.dto.PerformanceDateInfo(pd.uid, pd.showTime, count(ss), count(rs.seat)) FROM Performance p " +
            "JOIN p.performanceDateTime pd " +
            "JOIN p.performancePlace pp " +
            "JOIN pp.seats ss " +
            "LEFT JOIN Reservation rs ON ss = rs.seat " +
            "GROUP BY pd.uid")
    fun getDateInfo(performanceUid: String) : List<PerformanceDateInfo>
}