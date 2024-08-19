package com.flab.ticketing.performance.controller

import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.dto.response.PerformanceDateDetailResponse
import com.flab.ticketing.performance.dto.response.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.service.PerformanceService
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/performances")
class PerformanceController(
    private val performanceService: PerformanceService
) {


    @GetMapping
    fun getList(
        @ModelAttribute cursorInfoDto: CursorInfoDto,
        @ModelAttribute searchConditions: PerformanceSearchConditions
    ): CursoredResponse<PerformanceSummarySearchResult> {
        val performances = performanceService.search(
            CursorInfoDto(cursorInfoDto.cursor, cursorInfoDto.limit + 1),
            searchConditions
        )

        if (performances.size == cursorInfoDto.limit + 1) {
            return CursoredResponse(
                performances[cursorInfoDto.limit].uid,
                performances.dropLast(1)
            )
        }

        return CursoredResponse(null, performances)
    }

    @GetMapping("/{id}")
    fun getDetailPerformanceInfo(
        @PathVariable(name = "id") uid: String
    ): PerformanceDetailResponse {
        return performanceService.searchDetail(uid)
    }

    @GetMapping("/{performanceId}/dates/{dateId}")
    fun getPerformanceDateSeatInfo(
        @PathVariable("performanceId") performanceUid: String,
        @PathVariable("dateId") dateUid: String
    ): PerformanceDateDetailResponse {
        return performanceService.getPerformanceSeatInfo(
            performanceUid,
            dateUid
        )
    }

}