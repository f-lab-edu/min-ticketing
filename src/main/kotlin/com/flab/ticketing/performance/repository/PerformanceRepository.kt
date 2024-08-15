package com.flab.ticketing.performance.repository

import com.flab.ticketing.performance.dto.PerformanceDateInfo
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.repository.dsl.CustomPerformanceRepository
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PerformanceRepository : CustomPerformanceRepository,
    org.springframework.data.repository.Repository<Performance, Long> {
    fun save(performance: Performance)

    fun deleteAll()
    @EntityGraph(attributePaths = ["performancePlace", "performanceDateTime"])
    fun findByUid(uid : String): Performance?



    @Query("SELECT new com.flab.ticketing.performance.dto.PerformanceDateInfo(pd.uid, pd.showTime, count(ss), count(rs.seat)) FROM Performance p " +
            "JOIN p.performanceDateTime pd " +
            "JOIN p.performancePlace pp " +
            "JOIN pp.seats ss " +
            "LEFT JOIN Reservation rs ON ss = rs.seat " +
            "GROUP BY pd.uid")
    fun getDateInfo(performanceUid: String) : List<PerformanceDateInfo>
}