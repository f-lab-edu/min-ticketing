package com.flab.ticketing.performance.repository

import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.repository.dsl.CustomPerformanceRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PerformanceRepository : CustomPerformanceRepository, CrudRepository<Performance, Long>