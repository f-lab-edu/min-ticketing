package com.flab.ticketing.order.repository.dsl

import com.flab.ticketing.common.dto.service.CursorInfoDto
import com.flab.ticketing.order.dto.request.OrderSearchConditions
import com.flab.ticketing.order.entity.Order
import com.flab.ticketing.performance.entity.Performance
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class CustomOrderRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor
): CustomOrderRepository {

    override fun findByUser(
        userUid: String,
        cursorInfoDto: CursorInfoDto,
        searchConditions: OrderSearchConditions
    ): List<Order> {

        val searchResult = kotlinJdslJpqlExecutor.findPage(PageRequest.of(0, cursorInfoDto.limit)) {
            val cursorSubQuery = select<Long>(path(Order::id))
                .from(entity(Order::class))
                .where(
                    path(Order::uid).eq(cursorInfoDto.cursor)
                ).asSubquery()

            select(entity(Order::class))
                .from(entity(Order::class))
                .where(and(
                    cursorInfoDto.cursor?.let {
                        path(Order::id).lessThanOrEqualTo(cursorSubQuery)
                    },
                    searchConditions.status?.let {
                        path(Order::status).eq(searchConditions.status)
                    }
                )).orderBy(
                    path(Order::id).desc()
                )
        }


        return searchResult.content.filterNotNull()
    }
}