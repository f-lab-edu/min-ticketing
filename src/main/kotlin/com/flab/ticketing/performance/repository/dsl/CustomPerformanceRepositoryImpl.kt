package com.flab.ticketing.performance.repository.dsl

import com.flab.ticketing.common.dto.CursorInfo
import com.flab.ticketing.common.entity.Region
import com.flab.ticketing.performance.dto.PerformanceDetailSearchResult
import com.flab.ticketing.performance.dto.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.PerformanceSearchResult
import com.flab.ticketing.performance.entity.Performance
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlace
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class CustomPerformanceRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor
) : CustomPerformanceRepository {
    override fun search(
        searchConditions: PerformanceSearchConditions,
        cursorInfo: CursorInfo
    ): List<PerformanceSearchResult?> {

        val searchResult = kotlinJdslJpqlExecutor.findPage(PageRequest.of(0, cursorInfo.limit)) {
            selectNew<PerformanceSearchResult>(
                path(Performance::uid),
                path(Performance::image),
                path(Performance::name).`as`(expression("title")),
                path(Region::name).`as`(expression("regionName")),
                min(path(PerformanceDateTime::showTime)).`as`(expression("startDate")),
                max(path(PerformanceDateTime::showTime)).`as`(expression("endDate"))
            )
                .from(
                    entity(Performance::class),
                    join(entity(PerformanceDateTime::class)).on(
                        path(PerformanceDateTime::performance).eq(
                            entity(Performance::class)
                        )
                    ),
                    join(entity(PerformancePlace::class)).on(
                        path(Performance::performancePlace).eq(
                            entity(PerformancePlace::class)
                        )
                    ),
                    join(entity(Region::class)).on(
                        path(PerformancePlace::region).eq(
                            entity(Region::class)
                        )
                    )
                )
                .where(
                    and(
                        searchConditions.region?.let {
                            path(Region::uid).eq(it)
                        },
                        searchConditions.minPrice?.let {
                            path(Performance::price).greaterThanOrEqualTo(it)
                        },
                        searchConditions.maxPrice?.let {
                            path(Performance::price).lessThanOrEqualTo(it)
                        },
                        searchConditions.showTime?.let {
                            path(PerformanceDateTime::showTime).between(
                                it.toLocalDate().atStartOfDay(it.zone),
                                it.toLocalDate().plusDays(1).atStartOfDay(it.zone)
                            )
                        }
                    )
                )
                .groupBy(path(PerformanceDateTime::performance))
                .orderBy(
                    path(Performance::id).desc()
                )
        }

        return searchResult.content
    }

    override fun searchDetail(uid: String): PerformanceDetailSearchResult {
        TODO("Not yet implemented")
    }

}