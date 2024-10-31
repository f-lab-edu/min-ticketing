package com.flab.ticketing.performance.repository

import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import com.flab.ticketing.performance.repository.dsl.EsPerformanceSearchRepository
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository


@Repository
interface PerformanceSearchRepository :
    ElasticsearchRepository<PerformanceSearchSchema, String>,
    EsPerformanceSearchRepository