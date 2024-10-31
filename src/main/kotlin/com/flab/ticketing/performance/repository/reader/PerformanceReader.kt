package com.flab.ticketing.performance.repository.reader

import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.entity.Region
import com.flab.ticketing.common.exception.NotFoundException
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.performance.dto.service.PerformanceSearchResult
import com.flab.ticketing.performance.dto.service.PerformanceStartEndDateResult
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.exception.PerformanceErrorInfos
import com.flab.ticketing.performance.repository.PerformanceDateRepository
import com.flab.ticketing.performance.repository.PerformanceRepository
import com.flab.ticketing.performance.repository.PerformanceSearchRepository
import com.flab.ticketing.performance.repository.RegionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
@Logging
class PerformanceReader(
    private val performanceRepository: PerformanceRepository,
    private val performanceSearchRepository: PerformanceSearchRepository,
    private val performanceDateRepository: PerformanceDateRepository,
    private val regionRepository: RegionRepository
) {

    fun search(
        searchConditions: PerformanceSearchConditions,
        cursor: List<Any>?,
        limit: Int
    ): PerformanceSearchResult {
        return performanceSearchRepository.search(searchConditions, cursor, limit)
    }

    fun findPerformanceEntityByCursor(
        cursorInfoDto: CursorInfoDto
    ): List<Performance> {
        return performanceRepository.search(cursorInfoDto)
    }

    fun findPerformanceStartAndEndDate(
        performanceIdList: List<Long>
    ): List<PerformanceStartEndDateResult> {
        return performanceDateRepository.findStartAndEndDate(performanceIdList)
    }

    fun findPerformanceDetailDto(performanceUid: String): Performance {
        return performanceRepository.findByUid(performanceUid)
            ?: throw NotFoundException(PerformanceErrorInfos.PERFORMANCE_NOT_FOUND)
    }

    fun findPerformanceEntityByUidJoinWithPlace(performanceUid: String): Performance {
        return performanceRepository.findPerformanceByUidJoinWithPlaceAndSeat(performanceUid)
            ?: throw NotFoundException(
                PerformanceErrorInfos.PERFORMANCE_NOT_FOUND
            )
    }

    fun findDateSummaryDto(performanceId: Long): List<PerformanceDateSummaryResult> {
        return performanceDateRepository.getDateInfo(performanceId)
    }

    fun findDateEntityByUid(performanceUid: String, dateUid: String): PerformanceDateTime {
        return performanceDateRepository.findPerformanceDateTime(performanceUid, dateUid) ?: throw NotFoundException(
            PerformanceErrorInfos.PERFORMANCE_DATE_NOT_FOUND
        )
    }

    fun getRegions(): MutableIterable<Region> {
        return regionRepository.findAll()
    }

}