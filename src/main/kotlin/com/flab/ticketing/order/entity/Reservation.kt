package com.flab.ticketing.order.entity

import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import jakarta.persistence.*

@Entity
@Table(name = "reservations")
class Reservation(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_date_time")
    private val performanceDateTime: PerformanceDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_seat")
    private val seat: PerformancePlaceSeat,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order")
    private val order: Order
) : BaseEntity()