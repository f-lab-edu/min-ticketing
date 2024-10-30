package com.flab.ticketing.batch.migration

import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import com.flab.ticketing.performance.repository.PerformanceSearchRepository
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter

class PerformanceItemWriter(
    private val performanceSearchRepository: PerformanceSearchRepository
) : ItemWriter<PerformanceSearchSchema> {

    override fun write(chunk: Chunk<out PerformanceSearchSchema>) {
        performanceSearchRepository.saveAll(chunk.items)
    }
}