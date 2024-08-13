package com.flab.ticketing.performance.repository

import com.flab.ticketing.common.entity.Region
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RegionRepository : CrudRepository<Region, Long>