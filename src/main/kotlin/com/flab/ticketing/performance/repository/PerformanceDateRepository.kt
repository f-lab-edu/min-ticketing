package com.flab.ticketing.performance.repository

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
}