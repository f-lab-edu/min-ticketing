package com.flab.ticketing.performance.controller

import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.service.PerformanceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/v2/performances")
class PerformanceControllerV2(
    private val performanceService: PerformanceService
) {


    @Operation(
        summary = "공연 목록 조회 v2",
        description = "조건 없이 공연 목록을 조회합니다. 커서 기반 페이징을 지원합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "성공적으로 공연 목록을 조회함")
        ]
    )
    @GetMapping()
    fun getListV2(
        @ParameterObject @ModelAttribute cursorInfoDto: CursorInfoDto
    ): CursoredResponse<PerformanceSummarySearchResult> {
        val performances = performanceService.search(
            CursorInfoDto(cursorInfoDto.cursor, cursorInfoDto.limit + 1)
        )

        if (performances.size == cursorInfoDto.limit + 1) {
            return CursoredResponse(
                performances[cursorInfoDto.limit].uid,
                performances.dropLast(1)
            )
        }

        return CursoredResponse(null, performances)
    }
}