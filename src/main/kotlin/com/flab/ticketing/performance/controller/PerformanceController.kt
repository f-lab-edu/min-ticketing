package com.flab.ticketing.performance.controller

import com.flab.ticketing.common.dto.CursorInfo
import com.flab.ticketing.common.dto.CursoredResponse
import com.flab.ticketing.performance.dto.PerformanceDateInfoResult
import com.flab.ticketing.performance.dto.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.PerformanceSearchResult
import com.flab.ticketing.performance.service.PerformanceService
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/performances")
class PerformanceController(
    private val performanceService: PerformanceService
) {


    @GetMapping
    fun getList(
        @ModelAttribute cursorInfo: CursorInfo,
        @ModelAttribute searchConditions: PerformanceSearchConditions
    ): CursoredResponse<PerformanceSearchResult> {
        val performances = performanceService.search(
            CursorInfo(cursorInfo.cursor, cursorInfo.limit + 1),
            searchConditions
        )

        if (performances.size == cursorInfo.limit + 1) {
            return CursoredResponse(
                performances[cursorInfo.limit].uid,
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
    ): PerformanceDateInfoResult {
        return performanceService.getPerformanceSeatInfo(
            performanceUid,
            dateUid
        )
    }

}