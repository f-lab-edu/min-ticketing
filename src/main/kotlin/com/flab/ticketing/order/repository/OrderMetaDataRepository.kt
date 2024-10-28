package com.flab.ticketing.order.repository

import com.flab.ticketing.order.entity.OrderMetaData
import org.springframework.data.repository.CrudRepository

interface OrderMetaDataRepository : CrudRepository<OrderMetaData, String> 