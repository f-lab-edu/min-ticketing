package com.flab.ticketing.batch.migration

import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import org.springframework.batch.item.ItemProcessor

class PerformanceConvertItemProcessor : ItemProcessor<Performance, PerformanceSearchSchema> {

    override fun process(item: Performance): PerformanceSearchSchema {
        return PerformanceSearchSchema.of(item)
    }
}