package com.flab.ticketing.performance.repository.dsl

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery
import co.elastic.clients.json.JsonData
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.dto.service.PerformanceSearchResult
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.search
import java.time.format.DateTimeFormatter

class EsPerformanceSearchRepositoryImpl(
    private val elasticsearchOperations: ElasticsearchOperations
) : EsPerformanceSearchRepository {

    override fun search(
        searchConditions: PerformanceSearchConditions,
        sortValues: List<Any>?,
        limit: Int
    ): PerformanceSearchResult {
        val query = BoolQuery.Builder()
            .setUpQueries(searchConditions)
            .build()
            ._toQuery()

        val nativeQuery = NativeQueryBuilder()
            .withQuery(query)
            .withSearchAfter(sortValues)
            .withPageable(PageRequest.of(0, limit))
            .withSort(Sort.by(Sort.Order.desc("_score"), Sort.Order.asc("id")))
            .build()

        val searchHits = elasticsearchOperations.search<PerformanceSearchSchema>(nativeQuery).searchHits

        val nextCursor = createNextCursor(searchHits, limit)

        return PerformanceSearchResult(nextCursor, searchHits.map { it.content })
    }

    private fun BoolQuery.Builder.setUpQueries(searchConditions: PerformanceSearchConditions): BoolQuery.Builder {

        // region 이름 검색 : {'bool' : [ {'match' : {'region' : '$regionName'} ] }
        searchConditions.region?.let {
            this.must(MatchQuery.Builder().field("region").query(it).build()._toQuery())
        }


        // 가격 범위 검색, 둘중 하나만 있어도 검색이 가능합니다.:
        // { "bool":
        //  { "must": [
        //     { "range": {
        //         "price": {
        //             "gte": $minPrice,
        //             "lte": $maxPrice
        //         }
        //     } }
        // ] } }
        with(searchConditions) {
            if (minPrice == null && maxPrice == null) {
                return@with
            }
            val priceRangeQuery = RangeQuery.Builder().field("price").apply {
                minPrice?.let { gte(JsonData.of(minPrice)) }
                maxPrice?.let { lte(JsonData.of(maxPrice)) }
            }.build()._toQuery()

            this@setUpQueries.must(priceRangeQuery)

        }

        // 공연 시간 검색:
        // { "bool": { "must": [
        //     { "range": {
        //         "showTimes": {
        //             "gte": "2024-03-15T00:00:00+09:00",
        //             "lt": "2024-03-16T00:00:00+09:00"
        //         }
        //     } }
        // ] } }
        searchConditions.showTime?.let { time ->
            val startOfDay = time.toLocalDate().atStartOfDay(time.zone)
            val endOfDay = startOfDay.plusDays(1)

            // showTimes 배열의 요소 중 하나라도 해당 날짜에 속하면 매칭
            val rangeQuery = RangeQuery.Builder()
                .field("showTimes")
                .gte(JsonData.of(startOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .lt(JsonData.of(endOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .build()._toQuery()

            this.must(rangeQuery)
        }

        // 제목 검색, match_phrase를 사용해서 단어의 "포함" 뿐 아니라 단어간의 순서도 일치해야 합니다.:
        // { "bool": { "must": [
        //     { "match_phrase": { "title": "$searchText" } }
        // ] } }
        searchConditions.q?.let {
            this.must(
                MatchPhraseQuery.Builder().field("title").query(it).build()._toQuery()
            )
        }

        return this
    }


    private fun createNextCursor(searchHits : List<SearchHit<PerformanceSearchSchema>>, expectedSize: Int) : List<Any>?{
        if(searchHits.size == expectedSize){
            return searchHits.last().sortValues
        }
        return null
    }
}