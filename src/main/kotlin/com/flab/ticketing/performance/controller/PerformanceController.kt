package com.flab.ticketing.performance.controller

import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.common.dto.response.ErrorResponse
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.response.PerformanceDateDetailResponse
import com.flab.ticketing.performance.dto.response.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.service.PerformanceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/performances")
class PerformanceController(
    private val performanceService: PerformanceService
) {

    @Operation(
        summary = "공연 목록 조회",
        description = "여러 조건으로 공연 목록을 조회합니다. 커서 기반 페이징을 지원합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "성공적으로 공연 목록을 조회함")
        ]
    )
    @GetMapping
    fun getList(
        @ParameterObject @ModelAttribute cursorInfoDto: CursorInfoDto,
        @ParameterObject @ModelAttribute searchConditions: PerformanceSearchConditions
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

    @Operation(
        summary = "공연 목록 조회 v2",
        description = "조건 없이 공연 목록을 조회합니다. 커서 기반 페이징을 지원합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "성공적으로 공연 목록을 조회함")
        ]
    )
    @GetMapping("/v2")
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


    @Operation(
        summary = "공연 상세 정보 조회",
        description = "특정 공연의 상세 정보를 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "성공적으로 공연 상세 정보를 조회함"),
            ApiResponse(
                responseCode = "404", description = "공연을 찾을 수 없음 - PERFORMANCE-001",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/{id}")
    fun getDetailPerformanceInfo(
        @PathVariable(name = "id") uid: String
    ): PerformanceDetailResponse {
        return performanceService.searchDetail(uid)
    }


    @Operation(
        summary = "공연 날짜별 좌석 정보 조회",
        description = "특정 공연의 특정 날짜에 대한 좌석 정보를 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "성공적으로 좌석 정보를 조회함"),
            ApiResponse(
                responseCode = "400", description = "이미 지난 공연 - PERFORMANCE-003",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404", description = "공연 또는 공연 날짜를 찾을 수 없음 - PERFORMANCE-001",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
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