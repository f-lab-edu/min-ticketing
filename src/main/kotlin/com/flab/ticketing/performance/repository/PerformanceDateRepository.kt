package com.flab.ticketing.performance.repository

import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.performance.dto.service.PerformanceStartEndDateResult
import com.flab.ticketing.performance.entity.PerformanceDateTime
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface PerformanceDateRepository : CrudRepository<PerformanceDateTime, Long> {
    fun findByUid(uid: String): PerformanceDateTime?

    @Query(
        "SELECT pd FROM PerformanceDateTime pd " +
                "WHERE pd.performance.uid = :performanceUid " +
                "AND pd.uid = :dateUid"
    )
    fun findPerformanceDateTime(
        @Param("performanceUid") performanceUid: String,
        @Param("dateUid") dateUid: String
    ): PerformanceDateTime?

    @Query(
        "SELECT new com.flab.ticketing.performance.dto.service.PerformanceStartEndDateResult(" +
                "pd.performance.id, min(pd.showTime), max(pd.showTime)" +
                ") " +
                "from PerformanceDateTime pd " +
                "where pd.performance.id in :performanceIdList " +
                "group by pd.performance.id"
    )
    fun findStartAndEndDate(performanceIdList: List<Long>): List<PerformanceStartEndDateResult>

    @Query(
        "SELECT new com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult(pd.uid, pd.showTime, count(ss), count(rs.seat), count(c.seat)) FROM PerformanceDateTime pd " +
                "JOIN PerformancePlace pp ON pd.performance.performancePlace = pp " +
                "JOIN pp.seats ss " +
                "LEFT JOIN Reservation rs ON ss = rs.seat AND rs.performanceDateTime = pd " +
                "LEFT JOIN Cart c ON ss = c.seat AND c.performanceDateTime = pd " +
                "WHERE pd.performance.id = :performanceId " +
                "GROUP BY pd.id"
    )
    fun getDateInfo(@Param("performanceId") performanceId: Long): List<PerformanceDateSummaryResult>
}