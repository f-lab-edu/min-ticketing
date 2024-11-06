package com.flab.ticketing.performance.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.flab.ticketing.common.config.CacheConfig
import com.flab.ticketing.common.dto.response.CursoredResponse
import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.common.enums.CacheType
import com.flab.ticketing.common.utils.Base64Utils
import com.flab.ticketing.order.repository.reader.CartReader
import com.flab.ticketing.order.repository.reader.ReservationReader
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.response.PerformanceDateDetailResponse
import com.flab.ticketing.performance.dto.response.PerformanceDetailResponse
import com.flab.ticketing.performance.dto.response.RegionInfoResponse
import com.flab.ticketing.performance.dto.service.PerformanceDateSummaryResult
import com.flab.ticketing.performance.dto.service.PerformanceSummarySearchResult
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import com.flab.ticketing.performance.repository.reader.PerformanceReader
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class PerformanceService(
    private val performanceReader: PerformanceReader,
    private val reservationReader: ReservationReader,
    private val cartReader: CartReader,
    private val objectMapper: ObjectMapper
) {

    fun search(
        cursorInfoDto: CursorInfoDto,
        searchConditions: PerformanceSearchConditions
    ): CursoredResponse<PerformanceSummarySearchResult> {
        val decodedCursor = decodeCursor(cursorInfoDto.cursor)

        val (cursor, data) = performanceReader.search(searchConditions, decodedCursor, cursorInfoDto.limit)
        return CursoredResponse(
            encodeCursor(cursor),
            data.map { PerformanceSummarySearchResult.of(it) }
        )
    }


    @Caching(
        cacheable = [
            Cacheable(
                cacheManager = CacheConfig.COMPOSITE_CACHE_MANAGER_NAME,
                cacheNames = [CacheType.PRODUCT_CACHE_NAME],
                key = "'performance_' + (#cursorInfoDto.cursor ?: 'first_page') + '_' + #cursorInfoDto.limit"
            )
        ]
    )
    fun search(
        cursorInfoDto: CursorInfoDto
    ): List<PerformanceSummarySearchResult> {
        val performanceMap = performanceReader.findPerformanceEntityByCursor(cursorInfoDto)
            .associateBy { it.id }
            .toMap(LinkedHashMap())

        val startEndDateResultMap = performanceReader.findPerformanceStartAndEndDate(performanceMap.keys.toList())
            .associateBy { it.performanceId }

        return performanceMap.entries.map {
            val startEndDateResult = startEndDateResultMap[it.key]
            val performance = it.value
            PerformanceSummarySearchResult(
                performance.uid,
                performance.image,
                performance.name,
                performance.regionName,
                startEndDateResult?.startDate,
                startEndDateResult?.endDate
            )
        }

    }


    fun searchDetail(uid: String): PerformanceDetailResponse {
        val performance = performanceReader.findPerformanceDetailDto(uid)
        val dateSummaryDtoList = performanceReader.findDateSummaryDto(performance.id)
        val dateInfo = convertDateDtoToDateInfo(dateSummaryDtoList)

        return PerformanceDetailResponse(
            performance.uid,
            performance.image,
            performance.name,
            performance.regionName,
            performance.placeName,
            performance.price,
            performance.description,
            dateInfo
        )
    }

    fun getPerformanceSeatInfo(performanceUid: String, performanceDateUid: String): PerformanceDateDetailResponse {
        val performance = performanceReader.findPerformanceEntityByUidJoinWithPlace(performanceUid)
        val performanceDateTime = performanceReader.findDateEntityByUid(performanceUid, performanceDateUid)

        performanceDateTime.checkPassed()

        val reservedSeatUidList =
            reservationReader.findReserveUidInPlace(performance.performancePlace.id, performanceDateUid)

        val cartSeatUidList = cartReader.findSeatUidInPlace(performance.performancePlace.id, performanceDateUid)

        val seatTable = createSeatTable(
            performanceSeats = performance.performancePlace.seats,
            reservedSeatUidList = reservedSeatUidList,
            cartSeatUidList = cartSeatUidList
        )

        return PerformanceDateDetailResponse(
            performanceDateUid,
            performance.price,
            seatTable
        )
    }


    private fun createSeatTable(
        performanceSeats: List<PerformancePlaceSeat>,
        reservedSeatUidList: List<String>,
        cartSeatUidList: List<String>
    ): List<List<PerformanceDateDetailResponse.SeatInfo>> {
        val orderedDateSeatInfo = mutableListOf<MutableList<PerformanceDateDetailResponse.SeatInfo>>()

        performanceSeats.sortedWith(
            compareBy<PerformancePlaceSeat> { it.rowNum }
                .thenBy { it.columnNum })
            .forEach {
                if (orderedDateSeatInfo.size <= it.rowNum - 1) {
                    orderedDateSeatInfo.add(mutableListOf())
                }
                orderedDateSeatInfo[it.rowNum - 1].add(
                    PerformanceDateDetailResponse.SeatInfo(
                        it.uid,
                        it.name,
                        reservedSeatUidList.contains(it.uid) || cartSeatUidList.contains(it.uid)
                    )
                )
            }
        return orderedDateSeatInfo
    }

    private fun convertDateDtoToDateInfo(dtoList: List<PerformanceDateSummaryResult>): List<PerformanceDetailResponse.DateInfo> {
        return dtoList.map {
            PerformanceDetailResponse.DateInfo(
                it.uid,
                it.showTime.toLocalDateTime(),
                it.totalSeats,
                it.totalSeats - it.reservedSeats - it.cartSeats
            )
        }
    }


    private fun decodeCursor(cursor: String?): List<Any>? {
        if (cursor == null) return null

        return objectMapper.readValue<List<Any>>(Base64Utils.decode(cursor))
    }

    private fun encodeCursor(cursor: List<Any>?): String? {
        if (cursor == null) {
            return null
        }

        val notEncoded = objectMapper.writeValueAsString(cursor)
        return Base64Utils.encode(notEncoded)
    }


    @Caching(
        cacheable = [
            Cacheable(
                cacheManager = CacheConfig.COMPOSITE_CACHE_MANAGER_NAME,
                cacheNames = [CacheType.REGION_CACHE_NAME],
                key = "'region_list'"
            )
        ]
    )
    fun getRegions(): List<RegionInfoResponse> {
        return performanceReader.getRegions().map { RegionInfoResponse.of(it) }
    }
}