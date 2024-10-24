package com.flab.ticketing.performance.repository.dsl

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import com.flab.ticketing.performance.dto.request.PerformanceSearchConditions
import com.flab.ticketing.performance.entity.PerformanceSearchSchema
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.search

class CustomPerformanceSearchRepositoryImpl(
    private val elasticsearchOperations: ElasticsearchOperations
) : CustomPerformanceSearchRepository {

    override fun search(
        searchConditions: PerformanceSearchConditions,
        sortValues: List<Any>?,
        limit: Int
    ): Pair<List<Any>, List<PerformanceSearchSchema>> {
        val boolQueryBuilder = BoolQuery.Builder()
        val query = boolQueryBuilder.build()._toQuery()

        val nativeQuery = NativeQuery(query)
        nativeQuery.setPageable<NativeQuery>(PageRequest.of(0, limit))


        sortValues.let {
            nativeQuery.searchAfter = it
        }

        val searchHits = elasticsearchOperations.search<PerformanceSearchSchema>(nativeQuery).searchHits

        val nextCursor = searchHits.last().sortValues

        return Pair(nextCursor, searchHits.map { it.content })
    }
}