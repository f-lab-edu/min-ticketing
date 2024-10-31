package com.flab.ticketing.order.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash


@RedisHash(value = "order_meta_data", timeToLive = 30 * 60)
class OrderMetaData(

    @Id
    val orderId: String,
    val amount: Int,
    val cartUidList: List<String>,
    val userUid: String
)