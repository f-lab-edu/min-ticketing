package com.flab.ticketing.performance.repository

import com.flab.ticketing.performance.entity.PerformancePlace
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PerformancePlaceRepository : CrudRepository<PerformancePlace, Long>