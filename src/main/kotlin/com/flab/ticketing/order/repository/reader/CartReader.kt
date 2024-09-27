package com.flab.ticketing.order.repository.reader

import com.flab.ticketing.common.aop.Logging
import com.flab.ticketing.order.entity.Cart
import com.flab.ticketing.order.repository.CartRepository
import org.springframework.stereotype.Component


@Component
@Logging
class CartReader(
    private val cartRepository: CartRepository
) {

    fun findByUser(userUid: String): List<Cart> {
        return cartRepository.findByUserUid(userUid)
    }

    fun findSeatUidInPlace(placeId: Long, performanceDateTimeUid: String): List<String> {
        return cartRepository.findSeatUidByDateUidAndPlaceIn(performanceDateTimeUid, placeId)
    }

    fun findByUidList(uidList: List<String>): List<Cart> {
        return cartRepository.findByUidListInJoinWith(uidList)
    }
}