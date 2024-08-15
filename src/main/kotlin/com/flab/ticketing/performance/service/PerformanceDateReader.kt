package com.flab.ticketing.performance.service

import com.flab.ticketing.performance.dto.PerformanceDateInfo
import com.flab.ticketing.performance.repository.PerformanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class PerformanceDateReader(
    private val performanceRepository: PerformanceRepository
) {


    fun getDateInfo(performanceUid: String) : List<PerformanceDateInfo>{
        return performanceRepository.getDateInfo(performanceUid)
    }

}