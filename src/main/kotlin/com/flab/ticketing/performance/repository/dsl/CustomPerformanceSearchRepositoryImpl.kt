package com.flab.ticketing.performance.repository.dsl

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery
import co.elastic.clients.json.JsonData
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.search
import java.time.format.DateTimeFormatter

class CustomPerformanceSearchRepositoryImpl(
    private val elasticsearchOperations: ElasticsearchOperations
) : CustomPerformanceSearchRepository {
    
    override fun search(
        searchConditions: PerformanceSearchConditions,
        sortValues: List<Any>?,
        limit: Int
    ): Pair<List<Any>, List<PerformanceSearchSchema>> {
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

        val nextCursor = searchHits.last().sortValues

        return Pair(nextCursor, searchHits.map { it.content })
    }

    private fun BoolQuery.Builder.setUpQueries(searchConditions: PerformanceSearchConditions): BoolQuery.Builder {
        searchConditions.region?.let {
            this.must(MatchQuery.Builder().field("region").query(it).build()._toQuery())
        }


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

        searchConditions.q?.let {
            this.must(
                MatchPhraseQuery.Builder().field("title").query(it).build()._toQuery()
            )
        }

        return this
    }
}