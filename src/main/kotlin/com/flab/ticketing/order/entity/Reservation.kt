package com.flab.ticketing.order.entity

import com.flab.ticketing.common.entity.BaseEntity
import com.flab.ticketing.performance.entity.PerformanceDateTime
import com.flab.ticketing.performance.entity.PerformancePlaceSeat
import jakarta.persistence.*

@Entity
@Table(name = "reservations")
class Reservation : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_date_time")
    private lateinit var performanceDateTime: PerformanceDateTime

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_seat")
    private lateinit var seat: PerformancePlaceSeat


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order")
    private lateinit var order: Order
}