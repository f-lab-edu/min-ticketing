package com.flab.ticketing.performance.repository.dsl

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.entity.Region
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
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
        cursorInfoDto: CursorInfoDto
    ): List<PerformanceSummarySearchResult?> {

        val searchResult = kotlinJdslJpqlExecutor.findAll(PageRequest.of(0, cursorInfoDto.limit)) {

            val cursorSubQuery = select<Long>(path(Performance::id))
                .from(entity(Performance::class))
                .where(
                    path(Performance::uid).eq(cursorInfoDto.cursor)
                ).asSubquery()

            val searchShowTime = searchConditions.showTime

            val dateSubQuery = selectDistinct<Performance>(path(PerformanceDateTime::performance))
                .from(entity(PerformanceDateTime::class))
                .where(
                    path(PerformanceDateTime::showTime).between(
                        searchShowTime?.toLocalDate()?.atStartOfDay(searchShowTime.zone),
                        searchShowTime?.toLocalDate()?.plusDays(1)?.atStartOfDay(searchShowTime.zone)
                    )
                )
                .asSubquery()

            selectNew<PerformanceSummarySearchResult>(
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
                            entity(Performance::class).`in`(dateSubQuery)
                        },
                        searchConditions.q?.let {
                            path(Performance::name).like("%${it}%")
                        },
                        cursorInfoDto.cursor?.let {
                            path(Performance::id).lessThanOrEqualTo(cursorSubQuery)
                        }
                    )
                )
                .groupBy(path(PerformanceDateTime::performance))
                .orderBy(
                    path(Performance::id).desc()
                )
        }

        return searchResult
    }

    override fun search(cursorInfoDto: CursorInfoDto): List<Performance> {
        return kotlinJdslJpqlExecutor.findAll(PageRequest.of(0, cursorInfoDto.limit)) {
            val cursorSubQuery = select<Long>(path(Performance::id))
                .from(entity(Performance::class))
                .where(
                    path(Performance::uid).eq(cursorInfoDto.cursor)
                ).asSubquery()

            select(entity(Performance::class))
                .from(
                    entity(Performance::class),
                    fetchJoin(Performance::performancePlace),
                    fetchJoin(PerformancePlace::region)
                ).where(
                    cursorInfoDto.cursor?.let {
                        path(Performance::id).lessThanOrEqualTo(cursorSubQuery)
                    }
                ).orderBy(
                    path(Performance::id).desc()
                )
        }.filterNotNull()
    }
}