package com.flab.ticketing.performance.repository.dsl

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.entity.Performance
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class CustomPerformanceRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor
) : CustomPerformanceRepository {
    override fun search(cursorInfoDto: CursorInfoDto): List<Performance> {
        val t = kotlinJdslJpqlExecutor.findAll(PageRequest.of(0, cursorInfoDto.limit)) {
            val cursorSubQuery = select<Long>(path(Performance::id))
                .from(entity(Performance::class))
                .where(
                    path(Performance::uid).eq(cursorInfoDto.cursor)
                ).asSubquery()

            select(entity(Performance::class))
                .from(
                    entity(Performance::class)
                ).where(
                    cursorInfoDto.cursor?.let {
                        path(Performance::id).lessThanOrEqualTo(cursorSubQuery)
                    }
                ).orderBy(
                    path(Performance::id).desc()
                )
        }

        return t.filterNotNull()
    }
}