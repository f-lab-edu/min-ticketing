package com.flab.ticketing.order.entity

import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import jakarta.persistence.*

@Entity
@Table(name = "reservations")
class Reservation(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_date_time_id")
    private val performanceDateTime: PerformanceDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_seat_id")
    private val seat: PerformancePlaceSeat,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private val order: Order
) : BaseEntity()